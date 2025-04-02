package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static java.util.Collections.newSetFromMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.isBridgeMethodOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassResult.RELOCATED;
import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;
import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_RECORD;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
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
import name.remal.gradle_plugins.classes_relocation.relocator.minimization.ClassReachabilityConfigs;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport;
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

    private static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";

    private static final MethodKey WRITE_OBJECT_METHOD_KEY = methodKeyOf(
        "writeObject",
        getMethodDescriptor(VOID_TYPE, getType(ObjectOutputStream.class))
    );

    private static final MethodKey READ_OBJECT_METHOD_KEY = methodKeyOf(
        "readObject",
        getMethodDescriptor(VOID_TYPE, getType(ObjectInputStream.class))
    );

    private static final MethodKey READ_OBJECT_NO_DATA_METHOD_KEY = methodKeyOf("readObjectNoData", "()");

    private static final MethodKey WRITE_REPLACE_METHOD_KEY = methodKeyOf("writeReplace", "()");

    private static final MethodKey READ_RESOLVE_METHOD_KEY = methodKeyOf("readResolve", "()");


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
                var inputClassInternalName = relocatedClassData.getInputClassInternalName();
                var remapper = new RelocationRemapper(
                    inputClassInternalName,
                    relocatedClassData.getResource(),
                    fasterContext
                );

                var fieldName = task.getFields().poll();
                if (fieldName != null) {
                    var reachabilityReportContext = context.getRelocationComponent(ReachabilityReport.class)
                        .field(inputClassInternalName, fieldName)
                        .wrapRelocationContext(fasterContext);
                    relocateField(relocatedClassData, fieldName, classInfo, remapper, reachabilityReportContext);
                }

                var methodKey = task.getMethods().poll();
                if (methodKey != null) {
                    var reachabilityReportContext = context.getRelocationComponent(ReachabilityReport.class)
                        .method(inputClassInternalName, methodKey)
                        .wrapRelocationContext(fasterContext);
                    relocateMethod(relocatedClassData, methodKey, classInfo, remapper, reachabilityReportContext);
                }
            }
        }

        return Optional.of(RELOCATED);
    }

    private List<RelocatedClassData> createRelocatedClassDataList(String classInternalName, RelocationContext context) {
        var reachabilityReportContext = context.getRelocationComponent(ReachabilityReport.class)
            .clazz(classInternalName)
            .wrapRelocationContext(context);

        var classResources = context.getRelocationClasspath().getClassResources(classInternalName);
        return classResources.stream()
            .map(reachabilityReportContext::withResourceMarkedAsProcessed)
            .map(resource -> createRelocatedClassData(resource, classInternalName, reachabilityReportContext))
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

        relocateMethodsDefinedInNonRelocationParentClasses(relocatedClassData, classInfo, context);

        relocateMethodsAlreadyRelocatedInParentClasses(relocatedClassData, classInfo, context);

        relocateMembersAccordingToClassReachability(relocatedClassData, context);

        relocateMembersAnnotatedWithConfiguredAnnotations(relocatedClassData, context);

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
        }

        if ((relocatedClassData.getInputClassNode().access & ACC_ENUM) != 0) {
            context.queue(new RelocateMethod(classInternalName, methodKeyOf("values", "()")));

            relocatedClassData.getInputClassNode().fields.stream()
                .filter(fieldNode -> (fieldNode.access & ACC_STATIC) != 0)
                .map(fieldNode -> fieldNode.name)
                .filter(not(relocatedClassData::hasProcessedField))
                .forEach(fieldName -> context.queue(new RelocateField(classInternalName, fieldName)));
        }

        if ((relocatedClassData.getInputClassNode().access & ACC_RECORD) != 0) {
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

    private void relocateMethodsDefinedInNonRelocationParentClasses(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        classInfo.getAllParentClasses().stream()
            .filter(info -> !context.isRelocationClassInternalName(info.getInternalClassName()))
            .map(ClassInfo::getAccessibleMethods)
            .flatMap(Collection::stream)
            .filter(classInfo::hasAccessibleMethod)
            .filter(not(relocatedClassData::hasProcessedMethod))
            .distinct()
            .forEach(methodKey -> {
                context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), methodKey));
            });
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
            .distinct()
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
                || classInternalName.equals(config.getOnReachedClassInternalName())
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

                config.getFields().stream()
                    .filter(classInfo::hasField)
                    .forEach(fieldName -> context.queue(new RelocateField(classInternalName, fieldName)));

                config.getMethodsKeys().stream()
                    .filter(classInfo::hasMethod)
                    .forEach(methodKey -> context.queue(new RelocateMethod(classInternalName, methodKey)));

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

    private void relocateMembersAnnotatedWithConfiguredAnnotations(
        RelocatedClassData relocatedClassData,
        RelocationContext context
    ) {
        var keepAnnotationsFilter = context.getConfig().getMinimization().getKeepAnnotationsFilter();
        if (keepAnnotationsFilter.isEmpty()) {
            return;
        }

        relocatedClassData.getInputClassNode().methods.forEach(methodNode -> {
            var hasKeepAnnotation = Stream.of(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(annotationNode -> annotationNode.desc)
                .map(annotationDescriptor -> annotationDescriptor.substring(1, annotationDescriptor.length() - 1))
                .anyMatch(keepAnnotationsFilter::matches);
            if (hasKeepAnnotation) {
                context.queue(
                    new RelocateMethod(relocatedClassData.getInputClassInternalName(), methodKeyOf(methodNode))
                );
            }
        });
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


        if ((inputFieldNode.access & ACC_STATIC) != 0) {
            return;
        }

        // relocate all constructors if needed
        relocateAllConstructorsIfNeeded(relocatedClassData, classInfo, context);

        // relocate serialization if needed
        relocateSerializationMembersIfNeeded(relocatedClassData, classInfo, context);
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

        // if any of the relocated methods are instance methods:


        var inputOverrideableMethodNodes = inputMethodNodes.stream()
            .filter(RelocateClassCombinedImmediateHandler::isOverrideableMethod)
            .collect(toList());
        if (inputOverrideableMethodNodes.isEmpty()) {
            relocatedClassData.getRelocatedNonOverrideableMethods().add(methodKey);
        } else {
            relocatedClassData.getRelocatedOverrideableMethods().add(methodKey);
        }


        // relocate all constructors if needed
        relocateAllConstructorsIfNeeded(relocatedClassData, classInfo, context);

        // relocate serialization if needed
        relocateSerializationMembersIfNeeded(relocatedClassData, classInfo, context);

        // relocate overrideable methods from non-relocation classes if needed
        relocateOverrideableMethodsFromNonRelocationClassesIfNeeded(relocatedClassData, classInfo, context);


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

    private void relocateAllConstructorsIfNeeded(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        if (!relocatedClassData.getRelocatedConstructors().compareAndSet(false, true)) {
            return;
        }

        classInfo.getConstructors().stream()
            .filter(not(relocatedClassData::hasProcessedMethod))
            .forEach(otherMethodKey -> {
                context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), otherMethodKey));
            });
    }

    private void relocateSerializationMembersIfNeeded(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        if (!relocatedClassData.getRelocatedSerialization().compareAndSet(false, true)) {
            return;
        }

        Stream.of(
                SERIAL_VERSION_UID_FIELD_NAME
            )
            .filter(classInfo::hasField)
            .filter(not(relocatedClassData::hasProcessedField))
            .forEach(fieldName -> {
                context.queue(new RelocateField(relocatedClassData.getInputClassInternalName(), fieldName));
            });

        Stream.of(
                WRITE_OBJECT_METHOD_KEY,
                READ_OBJECT_METHOD_KEY,
                READ_OBJECT_NO_DATA_METHOD_KEY,
                WRITE_REPLACE_METHOD_KEY,
                READ_RESOLVE_METHOD_KEY
            )
            .filter(classInfo::hasMethod)
            .filter(not(relocatedClassData::hasProcessedMethod))
            .forEach(otherMethodKey -> {
                context.queue(new RelocateMethod(relocatedClassData.getInputClassInternalName(), otherMethodKey));
            });
    }

    private void relocateOverrideableMethodsFromNonRelocationClassesIfNeeded(
        RelocatedClassData relocatedClassData,
        ClassInfo classInfo,
        RelocationContext context
    ) {
        if (!relocatedClassData.getRelocatedOverrideableMethodsFromNonRelocationClasses().compareAndSet(false, true)) {
            return;
        }

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
