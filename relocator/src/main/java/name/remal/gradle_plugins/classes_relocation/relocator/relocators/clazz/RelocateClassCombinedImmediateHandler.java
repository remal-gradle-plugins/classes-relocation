package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.isBridgeMethodOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassResult.RELOCATED;
import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_RECORD;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.FilterClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.MinimalFieldsAndMethodsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfo;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.reachability.ClassReachabilityConfigs;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;
import org.jetbrains.annotations.Contract;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@CustomLog
@SuppressWarnings("java:S1121")
public class RelocateClassCombinedImmediateHandler
    implements ImmediateTaskHandler<RelocateClassResult, RelocateClassCombined> {

    private final Map<String, List<RelocatedClassData>> relocatedClassDataMap = isDebugEnabled()
        ? new TreeMap<>()
        : new LinkedHashMap<>();

    @Override
    public void finalizeRelocation(RelocationContext context) {
        var relocatedClassInternalNamePrefix = context.getRelocatedClassInternalNamePrefix();

        var relocatedClassDataList = relocatedClassDataMap.values().stream()
            .flatMap(Collection::stream)
            .collect(toList());
        for (var data : relocatedClassDataList) {
            ClassVisitor classVisitor;
            var classWriter = (ClassWriter) (classVisitor = new ClassWriter(0));

            classVisitor = wrapWithTestClassVisitors(classVisitor);

            classVisitor = new FilterClassVisitor(classVisitor, classInternalName -> {
                if (classInternalName.startsWith(relocatedClassInternalNamePrefix)) {
                    var originalClassInternalName = classInternalName.substring(
                        relocatedClassInternalNamePrefix.length()
                    );
                    if (context.isRelocationClassInternalName(originalClassInternalName)) {
                        if (relocatedClassDataMap.containsKey(originalClassInternalName)) {
                            return true;
                        } else {
                            logger.debug(
                                "Remove a reference to `{}` class from `{}`",
                                classInternalName,
                                data.getOutputClassInternalName()
                            );
                            return false;
                        }
                    }
                }

                return true;
            });

            data.getOutputClassNode().accept(classVisitor);
            var content = classWriter.toByteArray();

            var relocatedResource = newGeneratedResource(builder -> builder
                .withSourceResource(data.getResource())
                .withName(data.getOutputClassInternalName() + ".class")
                .withContent(content)
            );
            context.writeToOutput(relocatedResource);
            context.registerOriginalResource(relocatedResource, data.getResource());
        }
    }


    @Override
    public Optional<RelocateClassResult> handle(
        RelocateClassCombined task,
        RelocationContext context
    ) throws Throwable {
        var fasterContext = new RelocateClassCombinedHandlerRelocationContext(task, relocatedClassDataMap, context);

        var relocatedClassDataList = relocatedClassDataMap.computeIfAbsent(
            task.getClassInternalName(),
            it -> createRelocatedClassDataList(it, fasterContext)
        );

        var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
            .getClassInfo(task.getClassInternalName(), context);

        while (!task.isEmpty()) {
            for (var relocatedClassData : relocatedClassDataList) {
                var remapper = new RelocationRemapper(
                    relocatedClassData.getInputClassInternalName(),
                    relocatedClassData.getResource(),
                    fasterContext
                );

                var fieldName = task.getFields().poll();
                if (fieldName != null) {
                    relocateField(relocatedClassData, fieldName, classInfo, remapper, fasterContext);
                }

                var methodKey = task.getMethods().poll();
                if (methodKey != null) {
                    relocateMethod(relocatedClassData, methodKey, classInfo, remapper, fasterContext);
                }
            }
        }

        return Optional.of(RELOCATED);
    }

    private List<RelocatedClassData> createRelocatedClassDataList(String classInternalName, RelocationContext context) {
        var classResources = context.getRelocationClasspath().getClassResources(classInternalName);
        return classResources.stream()
            .map(context::withResourceMarkedAsProcessed)
            .map(resource -> createRelocatedClassData(resource, classInternalName, context))
            .collect(toList());
    }

    private RelocatedClassData createRelocatedClassData(
        Resource resource,
        String classInternalName,
        RelocationContext context
    ) {
        var inputClassNode = parseInputClassNode(resource);

        var outputClassNode = createOutputClassNode(inputClassNode, resource, context);

        var relocatedClassData = new RelocatedClassData(inputClassNode.name, resource, inputClassNode, outputClassNode);

        outputClassNode.fields.forEach(fieldNode -> {
            relocatedClassData.getRelocatedFields().add(fieldNode.name);
        });

        outputClassNode.methods.forEach(methodNode -> {
            var methodKey = methodKeyOf(methodNode);
            if (isOverrideableMethod(methodNode)) {
                relocatedClassData.getRelocatedOverrideableMethods().add(methodKey);
            } else {
                relocatedClassData.getRelocatedNonOverrideableMethods().add(methodKey);
            }
        });


        // default relocations:
        var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
            .getClassInfo(classInternalName, context);

        relocateGeneralMembers(relocatedClassData, classInfo, context);

        relocateMethodsAlreadyRelocatedInParentClasses(relocatedClassData, classInfo, context);

        relocateMembersAccordingToClassReachability(relocatedClassData, context);

        return relocatedClassData;
    }

    @SneakyThrows
    private ClassNode parseInputClassNode(Resource resource) {
        var inputClassNode = new ClassNode();
        try (var in = resource.open()) {
            new ClassReader(in).accept(inputClassNode, 0);
        }
        return inputClassNode;
    }

    private ClassNode createOutputClassNode(
        ClassNode inputClassNode,
        Resource resource,
        RelocationContext context
    ) {
        ClassVisitor classVisitor;
        ClassNode outputClassNode = (ClassNode) (classVisitor = new ClassNode());

        var relocationSource = context.getRelocationSource(resource);
        classVisitor = new RelocationAnnotationsClassVisitor(classVisitor, relocationSource);

        var remapper = new RelocationRemapper(inputClassNode.name, resource, context);
        classVisitor = new RelocationClassRemapper(classVisitor, remapper, context);

        classVisitor = new MinimalFieldsAndMethodsClassVisitor(classVisitor);

        inputClassNode.accept(classVisitor);

        return outputClassNode;
    }

    private void relocateOverriddenMethodsInKnownChildClasses(
        ClassInfo classInfo,
        MethodKey methodKey,
        RelocationContext context
    ) {
        classInfo.getAllChildClasses().stream()
            .filter(info -> context.isRelocationClassInternalName(info.getInternalClassName()))
            .filter(info -> info.hasAccessibleMethod(methodKey))
            .forEach(info -> context.queue(new RelocateMethod(info.getInternalClassName(), methodKey)));
    }

    private void relocateGeneralMembers(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        var classInternalName = relocatedClassData.getInputClassInternalName();

        if (!classInfo.areAllResolved()) {
            relocateAllMembers(relocatedClassData, classInfo, context);

        } else if ((relocatedClassData.getInputClassNode().access & ACC_ENUM) != 0) {
            context.queue(new RelocateMethod(classInternalName, methodKeyOf("values", "()")));

            relocatedClassData.getInputClassNode().fields.stream()
                .filter(fieldNode -> (fieldNode.access & ACC_STATIC) != 0)
                .map(fieldNode -> fieldNode.name)
                .filter(not(relocatedClassData::hasProcessedField))
                .forEach(fieldName -> context.queue(new RelocateField(classInternalName, fieldName)));

        } else if ((relocatedClassData.getInputClassNode().access & ACC_RECORD) != 0) {
            relocatedClassData.getInputClassNode().methods.stream()
                .filter(methodNode -> (methodNode.access & ACC_STATIC) == 0)
                .map(MethodKey::methodKeyOf)
                .filter(not(relocatedClassData::hasProcessedMethod))
                .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));
        }
    }

    private void relocateAllMembers(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        var classInternalName = relocatedClassData.getInputClassInternalName();

        classInfo.getFields().stream()
            .filter(not(relocatedClassData::hasProcessedField))
            .forEach(fieldName -> context.queue(new RelocateField(classInternalName, fieldName)));
        classInfo.getMethods().stream()
            .filter(not(relocatedClassData::hasProcessedMethod))
            .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));
    }

    private void relocateMethodsAlreadyRelocatedInParentClasses(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        classInfo.getAllParentClasses().stream()
            .filter(info -> context.isRelocationClassInternalName(info.getInternalClassName()))
            .map(info -> relocatedClassDataMap.get(info.getInternalClassName()))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(RelocatedClassData::getRelocatedOverrideableMethods)
            .flatMap(Collection::stream)
            .filter(classInfo::hasAccessibleMethod)
            .filter(not(relocatedClassData::hasProcessedMethod))
            .forEach(methodKey -> {
                context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), methodKey));
            });
    }

    private void relocateMembersAccordingToClassReachability(
        RelocatedClassData relocatedClassData,
        RelocationContext context
    ) {
        var classInternalName = relocatedClassData.getInputClassInternalName();
        relocateMembersForToEnabledClassReachability(classInternalName, context);

        // relocate classes enabled by this class:
        context.getRelocationComponent(ClassReachabilityConfigs.class)
            .getClassReachabilityConfigsEnabledByClass(classInternalName)
            .forEach(enabledConfig -> {
                var configClassInternalName = enabledConfig.getClassInternalName();
                context.queue(new RelocateClass(configClassInternalName));

                // force relocation of members if the enabled class was relocated earlier:
                if (relocatedClassDataMap.containsKey(configClassInternalName)) {
                    relocateMembersForToEnabledClassReachability(configClassInternalName, context);
                }
            });
    }

    private void relocateMembersForToEnabledClassReachability(
        String classInternalName,
        RelocationContext context
    ) {
        var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
            .getClassInfo(classInternalName, context);

        var classReachabilityConfigs = context.getRelocationComponent(ClassReachabilityConfigs.class);
        classReachabilityConfigs.getClassReachabilityConfigs(classInternalName).stream()
            .filter(config -> config.isAlwaysEnabled()
                || relocatedClassDataMap.containsKey(config.getOnReachedClassInternalName())
                || isRelocationQueued(config.getOnReachedClassInternalName(), context)
            )
            .forEach(config -> {
                classReachabilityConfigs.getNext().add(config.toBuilder()
                    .classInternalName(Optional.of(config.getClassInternalName())
                        .filter(context::isRelocationClassInternalName)
                        .map(name -> context.getRelocatedClassInternalNamePrefix() + name)
                        .orElseGet(config::getClassInternalName)
                    )
                    .onReachedClassInternalName(Optional.ofNullable(config.getOnReachedClassInternalName())
                        .filter(context::isRelocationClassInternalName)
                        .map(name -> context.getRelocatedClassInternalNamePrefix() + name)
                        .orElse(null)
                    )
                    .build()
                );

                config.getFields().forEach(fieldName ->
                    context.queue(new RelocateField(classInternalName, fieldName))
                );

                config.getMethodsKeys().forEach(methodKey ->
                    context.queue(new RelocateMethod(classInternalName, methodKey))
                );

                if (config.isAllDeclaredConstructors()) {
                    classInfo.getConstructors().stream()
                        .filter(MethodKey::isConstructor)
                        .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));

                } else if (config.isAllPublicConstructors()) {
                    classInfo.getAccessibleConstructors().stream()
                        .filter(MethodKey::isConstructor)
                        .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));
                }

                if (config.isAllDeclaredMethods()) {
                    classInfo.getMethods().stream()
                        .filter(not(MethodKey::isConstructor))
                        .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));

                } else if (config.isAllPublicMethods()) {
                    classInfo.getAccessibleMethods().stream()
                        .filter(not(MethodKey::isConstructor))
                        .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));
                }

                if (config.isAllDeclaredFields()) {
                    classInfo.getFields().forEach(fieldName ->
                        context.queue(new RelocateField(classInternalName, fieldName))
                    );

                } else if (config.isAllPublicFields()) {
                    classInfo.getAccessibleFields().forEach(fieldName ->
                        context.queue(new RelocateField(classInternalName, fieldName))
                    );
                }

                if (config.isAllPermittedSubclasses()) {
                    classInfo.getPermittedSubclassInternalNames().forEach(subclassInternalName ->
                        context.queue(new RelocateClass(subclassInternalName))
                    );
                }
            });
    }

    @Contract("null,_->false")
    private boolean isRelocationQueued(@Nullable String classInternalName, RelocationContext context) {
        if (classInternalName == null) {
            return false;
        }

        return context.hasTaskQueued(
            RelocateClassTask.class,
            task -> task.getClassInternalName().equals(classInternalName)
        );
    }

    private void relocateField(
        RelocatedClassData relocatedClassData,
        String fieldName,
        ClassInfo classInfo,
        RelocationRemapper remapper,
        RelocationContext context
    ) {
        if (relocatedClassData.hasProcessedField(fieldName)) {
            return;
        }

        var inputFieldNode = relocatedClassData.getInputClassNode().fields.stream()
            .filter(fieldNode -> fieldName.equals(fieldNode.name))
            .findFirst()
            .orElse(null);
        if (inputFieldNode == null) {
            logger.trace("Field `{}` can't be found in {}", fieldName, relocatedClassData.getResource());
            relocatedClassData.getNotFoundFields().add(fieldName);
            classInfo.getAllParentClasses().stream()
                .filter(info -> context.isRelocationClassInternalName(info.getInternalClassName()))
                .filter(info -> info.hasAccessibleField(fieldName))
                .forEach(info -> {
                    context.queue(new RelocateField(info.getInternalClassName(), fieldName));
                });
            return;
        }

        var classRemapper = new RelocationClassRemapper(relocatedClassData.getOutputClassNode(), remapper, context);
        inputFieldNode.accept(classRemapper);

        relocatedClassData.getRelocatedFields().add(fieldName);
    }

    private void relocateMethod(
        RelocatedClassData relocatedClassData,
        MethodKey methodKey,
        ClassInfo classInfo,
        RelocationRemapper remapper,
        RelocationContext context
    ) {
        if (relocatedClassData.hasProcessedMethod(methodKey)) {
            return;
        }

        var inputMethodNodes = relocatedClassData.getInputClassNode().methods.stream()
            .filter(methodKey::matches)
            .collect(toCollection(() -> newSetFromMap(new IdentityHashMap<>())));
        if (inputMethodNodes.isEmpty()) {
            logger.trace("Method `{}` can't be found in {}", methodKey, relocatedClassData.getResource());
            relocatedClassData.getNotFoundMethods().add(methodKey);
            classInfo.getAllParentClasses().stream()
                .filter(info -> context.isRelocationClassInternalName(info.getInternalClassName()))
                .filter(info -> info.hasAccessibleMethod(methodKey))
                .forEach(info -> {
                    context.queue(new RelocateMethod(info.getInternalClassName(), methodKey));
                });
            return;
        }

        var classRemapper = new RelocationClassRemapper(relocatedClassData.getOutputClassNode(), remapper, context);
        inputMethodNodes.forEach(methodNode -> methodNode.accept(classRemapper));


        var instanceMethodNodes = inputMethodNodes.stream()
            .filter(methodNode -> (methodNode.access & ACC_STATIC) == 0)
            .collect(toList());
        if (instanceMethodNodes.isEmpty()) {
            relocatedClassData.getRelocatedNonOverrideableMethods().add(methodKey);
            return;
        }

        var inputOverrideableMethodNodes = inputMethodNodes.stream()
            .filter(RelocateClassCombinedImmediateHandler::isOverrideableMethod)
            .collect(toList());
        if (inputOverrideableMethodNodes.isEmpty()) {
            relocatedClassData.getRelocatedNonOverrideableMethods().add(methodKey);
        } else {
            relocatedClassData.getRelocatedOverrideableMethods().add(methodKey);
        }


        // relocate bridge methods:
        relocatedClassData.getInputClassNode().methods.stream()
            .filter(not(inputMethodNodes::contains))
            .filter(methodNode ->
                inputMethodNodes.stream().anyMatch(inputMethodNode ->
                    isBridgeMethodOf(methodNode, relocatedClassData.getInputClassInternalName(), inputMethodNode)
                )
            )
            .map(MethodKey::methodKeyOf)
            .filter(not(relocatedClassData::hasProcessedMethod))
            .forEach(otherMethodKey ->
                context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), otherMethodKey))
            );


        // if any of the relocated methods are instance methods,
        // relocate overrideable methods from non-relocation classes
        if (relocatedClassData.getRelocatedOverrideableMethodsFromNonRelocationClasses().compareAndSet(false, true)) {
            classInfo.getAllParentClasses().stream()
                .filter(info -> !context.isRelocationClassInternalName(info.getInternalClassName()))
                .map(ClassInfo::getOverrideableMethods)
                .flatMap(Collection::stream)
                .filter(classInfo::hasAccessibleMethod)
                .filter(not(relocatedClassData::hasProcessedMethod))
                .forEach(otherMethodKey -> {
                    context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), otherMethodKey));
                });
        }


        // relocate overridden methods in parent classes:
        var inputOverriddenCandidateMethodNodes = inputMethodNodes.stream()
            .filter(RelocateClassCombinedImmediateHandler::isOverriddenCandidateMethod)
            .collect(toList());
        if (!inputOverriddenCandidateMethodNodes.isEmpty()) {
            classInfo.getAllParentClasses().stream()
                .filter(info -> context.isRelocationClassInternalName(info.getInternalClassName()))
                .filter(info -> info.hasAccessibleMethod(methodKey))
                .forEach(info -> {
                    context.queue(new RelocateMethod(info.getInternalClassName(), methodKey));
                });
        }


        // relocate overridden methods in known child classes:
        relocateOverriddenMethodsInKnownChildClasses(classInfo, methodKey, context);
    }

    private static boolean isOverriddenCandidateMethod(MethodNode methodNode) {
        return (methodNode.access & ACC_PRIVATE) == 0
            && (methodNode.access & ACC_STATIC) == 0
            && !methodNode.name.startsWith("<");
    }

    private static boolean isOverrideableMethod(MethodNode methodNode) {
        return (methodNode.access & ACC_PRIVATE) == 0
            && (methodNode.access & ACC_STATIC) == 0
            && (methodNode.access & ACC_FINAL) == 0
            && !methodNode.name.startsWith("<");
    }

}
