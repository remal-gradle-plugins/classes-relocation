package name.remal.gradle_plugins.classes_relocation.relocator.class_info;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.toolkit.ObjectUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassInfoComponent
    implements ClassesRelocatorComponent {

    private final Map<String, ClassInfo> cache = isDebugEnabled() ? new TreeMap<>() : new LinkedHashMap<>();

    public ClassInfo getClassInfo(String internalClassName, RelocationContext context) {
        ClassInfo result = cache.get(internalClassName);

        if (result == null) {
            result = retrieveClassInfo(internalClassName, context);
        }

        cache.putIfAbsent(internalClassName, result);

        return result;
    }

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    private ClassInfo retrieveClassInfo(String internalClassName, RelocationContext context) {
        var classResources = Stream.of(
                context.getRelocationClasspath(),
                context.getSourceClasspath(),
                context.getCompileAndRuntimeClasspath(),
                context.getSystemClasspath()
            )
            .map(classpath -> classpath.getClassResources(internalClassName))
            .filter(ObjectUtils::isNotEmpty)
            .findFirst()
            .orElse(emptyList());
        if (classResources.isEmpty()) {
            return ClassInfo.builder()
                .internalClassName(internalClassName)
                .resolved(false)
                .build();
        }

        var parentClassInternalNames = new LinkedHashSet<String>();
        var fields = new LinkedHashSet<String>();
        var accessibleFields = new LinkedHashSet<String>();
        var constructors = new LinkedHashSet<MethodKey>();
        var accessibleConstructors = new LinkedHashSet<MethodKey>();
        var methods = new LinkedHashSet<MethodKey>();
        var accessibleMethods = new LinkedHashSet<MethodKey>();
        var overrideableMethods = new LinkedHashSet<MethodKey>();
        var permittedSubclassInternalNames = new LinkedHashSet<String>();

        for (var classResource : classResources) {
            var classVisitor = new ClassVisitor(getLatestAsmApi(), null) {
                @Override
                public void visit(
                    int version,
                    int access,
                    String name,
                    @Nullable String signature,
                    @Nullable String superName,
                    @Nullable String[] interfaces
                ) {
                    if (superName != null) {
                        parentClassInternalNames.add(superName);
                    }
                    if (interfaces != null) {
                        parentClassInternalNames.addAll(List.of(interfaces));
                    }

                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Nullable
                @Override
                public FieldVisitor visitField(
                    int access,
                    @Nonnull String name,
                    @Nonnull String descriptor,
                    @Nullable String signature,
                    @Nullable Object value
                ) {
                    fields.add(name);

                    if ((access & ACC_PRIVATE) == 0) {
                        accessibleFields.add(name);
                    }

                    return null;
                }

                @Nullable
                @Override
                public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    @Nullable String signature,
                    @Nullable String[] exceptions
                ) {
                    var methodKey = methodKeyOf(name, descriptor);
                    if (name.equals("<init>")) {
                        constructors.add(methodKey);

                        if ((access & ACC_PRIVATE) == 0) {
                            accessibleConstructors.add(methodKey);
                        }

                    } else if (name.startsWith("<")) {
                        // do nothing

                    } else {
                        methods.add(methodKey);

                        if ((access & ACC_PRIVATE) == 0) {
                            accessibleMethods.add(methodKey);

                            if ((access & ACC_STATIC) == 0
                                && (access & ACC_FINAL) == 0
                            ) {
                                overrideableMethods.add(methodKey);
                            }
                        }
                    }

                    return null;
                }

                @Override
                public void visitPermittedSubclass(String permittedSubclass) {
                    permittedSubclassInternalNames.add(permittedSubclass);
                }
            };

            try (var in = classResource.open()) {
                new ClassReader(in).accept(classVisitor, SKIP_CODE | SKIP_DEBUG);
            }
        }

        var parentClasses = parentClassInternalNames.stream()
            .map(it -> getClassInfo(it, context))
            .collect(toList());

        var classInfo = ClassInfo.builder()
            .internalClassName(internalClassName)
            .fields(fields)
            .accessibleFields(accessibleFields)
            .constructors(constructors)
            .accessibleConstructors(accessibleConstructors)
            .methods(methods)
            .accessibleMethods(accessibleMethods)
            .overrideableMethods(overrideableMethods)
            .permittedSubclassInternalNames(permittedSubclassInternalNames)
            .parentClasses(parentClasses)
            .build();

        parentClasses.forEach(it -> it.addChildClass(classInfo));

        return classInfo;
    }

}
