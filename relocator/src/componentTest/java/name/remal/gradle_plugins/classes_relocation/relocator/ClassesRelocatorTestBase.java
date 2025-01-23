package name.remal.gradle_plugins.classes_relocation.relocator;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.io.File.pathSeparator;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.isLazyProxyInitialized;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.tryToDeleteRecursively;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.DEFLATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ResourcesFilter;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath;
import name.remal.gradle_plugins.toolkit.UrlUtils;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.AfterEach;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

@CustomLog
@SuppressWarnings("Slf4jSignOnlyFormat")
public abstract class ClassesRelocatorTestBase {

    private static final String TO_RELOCATE_SUBPACKAGE_NAME = "to_relocate";

    private static final Class<?> CLASS_THAT_SHOULD_NOT_BE_AVAILABLE_IN_TEST_CLASS_LOADER_NAME = ImmutableList.class;

    private static final String WRAPPED_LOGIC_CLASS_NAME_PREFIX = "wrapped.";


    protected final Path tempDirPath = asLazyProxy(Path.class, () ->
        createTempDirectory(getClass().getSimpleName() + '-')
    );

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void cleanTempDirPath() {
        if (isLazyProxyInitialized(tempDirPath)) {
            tryToDeleteRecursively(tempDirPath);
        }
    }


    @SneakyThrows
    protected void assertTestLogic(
        Class<? extends ClassesRelocatorTestLogic> logicClass,
        String... relocationLibraries
    ) {
        // check that available here:
        Class.forName(CLASS_THAT_SHOULD_NOT_BE_AVAILABLE_IN_TEST_CLASS_LOADER_NAME.getName());

        {
            // verify test logic without relocation
            var testLogicCtor = makeAccessible(logicClass.getDeclaredConstructor());
            var testLogic = (ClassesRelocatorTestLogic) testLogicCtor.newInstance();
            try {
                testLogic.assertTestLogic();
            } catch (Throwable e) {
                throw new AssertionError("Test logic fails even WITHOUT relocation", e);
            }
        }

        var sourceJarPath = createSourceJar(logicClass);
        var libJarPath = createLibJar(logicClass);
        var targetJarPath = createParentDirectories(tempDirPath.resolve("target.jar"));
        relocate(
            sourceJarPath,
            targetJarPath,
            Stream.concat(
                Stream.of(libJarPath),
                stream(relocationLibraries)
                    .map(ClassesRelocatorTestBase::getLibraryFilePaths)
                    .flatMap(Collection::stream)
            ).distinct().collect(toList())
        );
        executeRelocatedTestLogic(
            logicClass.getName(),
            targetJarPath,
            "relocation"
        );

        var wrappedSourceJar = createWrappedSourceJar(logicClass);
        var wrappedTargetJarPath = createParentDirectories(tempDirPath.resolve("wrapped-target.jar"));
        relocate(wrappedSourceJar, wrappedTargetJarPath, List.of(targetJarPath));
        executeRelocatedTestLogic(
            WRAPPED_LOGIC_CLASS_NAME_PREFIX + logicClass.getName(),
            wrappedTargetJarPath,
            "WRAPPED relocation"
        );
    }

    @SneakyThrows
    private Path createSourceJar(Class<? extends ClassesRelocatorTestLogic> logicClass) {
        var sourceJarPath = createParentDirectories(tempDirPath.resolve("source.jar"));
        try (
            var testClassesJar = ZipFile.builder().setPath(getTestClassesJarPath()).get();
            var sourceJar = new ZipArchiveOutputStream(sourceJarPath)
        ) {
            sourceJar.setMethod(DEFLATED);
            sourceJar.setLevel(BEST_COMPRESSION);
            sourceJar.setUseZip64(Zip64Mode.AsNeeded);
            sourceJar.setEncoding(UTF_8.name());

            var sourcePackage = packageNameOf(logicClass);

            var filter = new ResourcesFilter()
                .include("META-INF/**")
                .include(toClassInternalName(sourcePackage) + "/*");

            var entriesToCopy = list(testClassesJar.getEntries()).stream()
                .filter(not(ZipArchiveEntry::isDirectory))
                .filter(entry -> filter.matches(entry.getName()))
                .collect(toUnmodifiableList());

            for (var entryToCopy : entriesToCopy) {
                var sourceEntry = new ZipArchiveEntry(entryToCopy);
                sourceJar.putArchiveEntry(sourceEntry);
                try (var sourceIn = testClassesJar.getInputStream(entryToCopy)) {
                    sourceIn.transferTo(sourceJar);
                }
                sourceJar.closeArchiveEntry();
            }
        }
        return sourceJarPath;
    }

    @SneakyThrows
    private Path createLibJar(Class<? extends ClassesRelocatorTestLogic> logicClass) {
        var libJarPath = createParentDirectories(tempDirPath.resolve("lib.jar"));
        try (
            var testClassesJar = ZipFile.builder().setPath(getTestClassesJarPath()).get();
            var libJar = new ZipArchiveOutputStream(libJarPath)
        ) {
            libJar.setMethod(DEFLATED);
            libJar.setLevel(BEST_COMPRESSION);
            libJar.setUseZip64(Zip64Mode.AsNeeded);
            libJar.setEncoding(UTF_8.name());

            var sourcePackage = packageNameOf(logicClass);
            var toRelocatePackage = sourcePackage + '.' + TO_RELOCATE_SUBPACKAGE_NAME;

            {
                var failFilter = new ResourcesFilter()
                    .include(toClassInternalName(packageNameOf(logicClass)) + "/*/**")
                    .exclude(toClassInternalName(toRelocatePackage) + "/**");

                var failEntries = list(testClassesJar.getEntries()).stream()
                    .filter(not(ZipArchiveEntry::isDirectory))
                    .map(ZipArchiveEntry::getName)
                    .filter(failFilter::matches)
                    .collect(toUnmodifiableList());

                assertEquals(List.of(), failEntries, "The only allowed subpackage is " + TO_RELOCATE_SUBPACKAGE_NAME);
            }

            var filter = new ResourcesFilter()
                .include("META-INF/**")
                .include(toClassInternalName(toRelocatePackage) + "/**");

            var entriesToCopy = list(testClassesJar.getEntries()).stream()
                .filter(not(ZipArchiveEntry::isDirectory))
                .filter(entry -> filter.matches(entry.getName()))
                .collect(toUnmodifiableList());

            for (var entryToCopy : entriesToCopy) {
                var libEntry = new ZipArchiveEntry(entryToCopy);
                libJar.putArchiveEntry(libEntry);
                try (var sourceIn = testClassesJar.getInputStream(entryToCopy)) {
                    sourceIn.transferTo(libJar);
                }
                libJar.closeArchiveEntry();
            }
        }
        return libJarPath;
    }

    @SneakyThrows
    private Path createWrappedSourceJar(Class<? extends ClassesRelocatorTestLogic> logicClass) {
        var wrappedSourceJarPath = createParentDirectories(tempDirPath.resolve("wrapped-source.jar"));
        try (var wrappedSourceJar = new ZipArchiveOutputStream(wrappedSourceJarPath)) {
            wrappedSourceJar.setMethod(DEFLATED);
            wrappedSourceJar.setLevel(BEST_COMPRESSION);
            wrappedSourceJar.setUseZip64(Zip64Mode.AsNeeded);
            wrappedSourceJar.setEncoding(UTF_8.name());

            var classNode = new ClassNode();
            classNode.version = V1_8;
            classNode.access = ACC_PUBLIC;
            classNode.name = toClassInternalName(WRAPPED_LOGIC_CLASS_NAME_PREFIX) + getInternalName(logicClass);
            classNode.superName = getInternalName(Object.class);
            classNode.interfaces = List.of(getInternalName(ClassesRelocatorTestLogic.class));
            classNode.fields = new ArrayList<>();
            classNode.methods = new ArrayList<>();

            {
                var methodNode = new MethodNode(
                    ACC_PUBLIC,
                    "<init>",
                    getMethodDescriptor(
                        VOID_TYPE
                    ),
                    null,
                    null
                );
                classNode.methods.add(methodNode);

                var instructions = methodNode.instructions = new InsnList();

                instructions.add(new VarInsnNode(ALOAD, 0));
                instructions.add(new MethodInsnNode(
                    INVOKESPECIAL,
                    classNode.superName,
                    "<init>",
                    getMethodDescriptor(
                        VOID_TYPE
                    )
                ));

                instructions.add(new InsnNode(RETURN));
            }

            {
                var assertTestLogicMethod = ClassesRelocatorTestLogic.class.getMethod("assertTestLogic");

                var methodNode = new MethodNode(
                    ACC_PUBLIC,
                    assertTestLogicMethod.getName(),
                    getMethodDescriptor(assertTestLogicMethod),
                    null,
                    null
                );
                classNode.methods.add(methodNode);

                var instructions = methodNode.instructions = new InsnList();

                instructions.add(new TypeInsnNode(NEW, getInternalName(logicClass)));
                instructions.add(new InsnNode(DUP));
                instructions.add(new MethodInsnNode(
                    INVOKESPECIAL,
                    getInternalName(logicClass),
                    "<init>",
                    getMethodDescriptor(
                        VOID_TYPE
                    )
                ));

                instructions.add(new MethodInsnNode(
                    logicClass.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
                    getInternalName(logicClass),
                    assertTestLogicMethod.getName(),
                    getMethodDescriptor(assertTestLogicMethod)
                ));

                instructions.add(new InsnNode(RETURN));
            }

            var classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
            classNode.accept(wrapWithTestClassVisitors(classWriter));

            wrappedSourceJar.putArchiveEntry(new ZipArchiveEntry(classNode.name + ".class"));
            wrappedSourceJar.write(classWriter.toByteArray());
            wrappedSourceJar.closeArchiveEntry();
        }
        return wrappedSourceJarPath;
    }

    private void relocate(
        Path sourceJarPath,
        Path targetJarPath,
        List<Path> relocationLibraries
    ) {
        try (
            var relocator = ClassesRelocator.builder()
                .sourceJarPath(sourceJarPath)
                .relocationClasspathPaths(relocationLibraries)
                .reachabilityMetadataClasspathPaths(getLibraryFilePaths("graalvm-reachability-metadata"))
                .targetJarPath(targetJarPath)
                .basePackageForRelocatedClasses("relocated")
                .build()
        ) {
            relocator.relocate();
        }
    }

    @SneakyThrows
    private void executeRelocatedTestLogic(
        String logicClassName,
        Path targetJarPath,
        String description
    ) {
        var classLoaderUrls = Stream.of(targetJarPath)
            .map(UrlUtils::toUrl)
            .toArray(URL[]::new);
        var prevContextClassLoader = Thread.currentThread().getContextClassLoader();
        try (var classLoader = new TestLogicClassLoader(classLoaderUrls)) {
            Thread.currentThread().setContextClassLoader(classLoader);

            try {
                classLoader.loadClass(CLASS_THAT_SHOULD_NOT_BE_AVAILABLE_IN_TEST_CLASS_LOADER_NAME.getName());
                fail("Should not be available: " + CLASS_THAT_SHOULD_NOT_BE_AVAILABLE_IN_TEST_CLASS_LOADER_NAME);
            } catch (ClassNotFoundException ignored) {
                // OK
            }

            var relocatedTestLogicClass = classLoader.loadClass(logicClassName);
            var relocatedTestLogicCtor = makeAccessible(relocatedTestLogicClass.getDeclaredConstructor());
            var relocatedTestLogic = (ClassesRelocatorTestLogic) relocatedTestLogicCtor.newInstance();
            try {
                relocatedTestLogic.assertTestLogic();
            } catch (Throwable e) {
                throw new AssertionError("Test logic fails WITH " + description, e);
            }

        } finally {
            Thread.currentThread().setContextClassLoader(prevContextClassLoader);
        }
    }


    private static final List<String> ALWAYS_AVAILABLE_LIBRARIES = List.of(
        "junit-jupiter-api",
        "assertj-core",
        "asm"
    );

    private static final Set<String> TEST_LOGIC_CLASS_NAMES;

    static {
        var testLogicClassNames = new LinkedHashSet<String>();

        Stream.of(
            ClassesRelocatorTestLogic.class
        ).map(Class::getName).forEach(testLogicClassNames::add);

        ALWAYS_AVAILABLE_LIBRARIES.stream()
            .map(ClassesRelocatorTestBase::getLibraryFilePaths)
            .map(Classpath::newClasspathForPaths)
            .map(Classpath::getClassNames)
            .flatMap(Collection::stream)
            .forEach(testLogicClassNames::add);

        TEST_LOGIC_CLASS_NAMES = ImmutableSet.copyOf(testLogicClassNames);
    }


    private static Path getTestClassesJarPath() {
        var property = "test-classes-jar";
        var path = System.getProperty(property);
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("System property not set: " + property);
        }

        return Paths.get(path);
    }

    private static Collection<Path> getLibraryFilePaths(String libraryName) {
        var classpathString = System.getProperty(libraryName + "-classpath");
        if (classpathString == null) {
            throw new IllegalStateException("Unknown library: " + libraryName);
        }

        return Splitter.on(pathSeparator).splitToStream(classpathString)
            .filter(not(String::isEmpty))
            .distinct()
            .map(Paths::get)
            .collect(toImmutableSet());
    }

    private static final ClassLoader TEST_LOGIC_BASE_CLASS_LOADER_DELEGATE =
        defaultValue(ClassesRelocatorTestBase.class.getClassLoader(), getSystemClassLoader());

    private static final ClassLoader TEST_LOGIC_BASE_CLASS_LOADER = new ClassLoader(null) {
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException ignored) {
                    // do nothing
                }
            }

            if (clazz != null) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }

            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (TEST_LOGIC_CLASS_NAMES.contains(name)) {
                return TEST_LOGIC_BASE_CLASS_LOADER_DELEGATE.loadClass(name);
            }

            throw new ClassNotFoundException(name);
        }
    };

    private static class TestLogicClassLoader extends URLClassLoader {

        public TestLogicClassLoader(URL[] urls) {
            super(urls, TEST_LOGIC_BASE_CLASS_LOADER);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException ignored) {
                    // do nothing
                }
            }

            if (clazz != null) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }

            return super.loadClass(name, resolve);
        }

        static {
            registerAsParallelCapable();
        }

    }

}
