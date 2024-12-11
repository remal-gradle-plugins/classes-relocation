package name.remal.gradle_plugins.classes_relocation.intern;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.io.ByteStreams.copy;
import static java.io.File.pathSeparator;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toList;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.isLazyProxyInitialized;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.tryToDeleteRecursively;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.DEFLATED;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.UrlUtils;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.AfterEach;

public abstract class ClassesRelocatorTestBase {

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


    private static final List<String> ALWAYS_AVAILABLE_LIBRARIES = ImmutableList.of(
        "junit-jupiter-api",
        "assertj-core",
        "asm"
    );


    @SneakyThrows
    protected void assertTestLogic(
        Class<? extends ClassesRelocatorTestLogic> logicClass,
        String... relocationLibraries
    ) {
        val classThatShouldNotBeAvailableInTestClassLoader = ImmutableList.class;
        Class.forName(classThatShouldNotBeAvailableInTestClassLoader.getName()); // check that available here

        {
            // the test logic works without relocation
            val testLogicCtor = makeAccessible(logicClass.getDeclaredConstructor());
            val testLogic = (ClassesRelocatorTestLogic) testLogicCtor.newInstance();
            testLogic.assertTestLogic();
        }

        val sourceJarPath = createSourceJar(logicClass);
        val targetJarPath = createParentDirectories(tempDirPath.resolve("target.jar"));
        try (
            val relocator = ClassesRelocator.builder()
                .sourceJarPath(sourceJarPath)
                .relocationClasspathPaths(stream(relocationLibraries)
                    .map(ClassesRelocatorTestBase::getLibraryFilePaths)
                    .flatMap(Collection::stream)
                    .collect(toList())
                )
                .targetJarPath(targetJarPath)
                .basePackageForRelocatedClasses("relocated")
                .build()
        ) {
            relocator.relocate();
        }

        val classLoaderUrls = Stream.concat(
                Stream.of(targetJarPath),
                ALWAYS_AVAILABLE_LIBRARIES.stream()
                    .map(ClassesRelocatorTestBase::getLibraryFilePaths)
                    .flatMap(Collection::stream)
            )
            .distinct()
            .map(UrlUtils::toUrl)
            .toArray(URL[]::new);
        try (val classLoader = new TestLogicClassLoader(classLoaderUrls)) {
            try {
                classLoader.loadClass(classThatShouldNotBeAvailableInTestClassLoader.getName());
                fail("Should not be available: " + classThatShouldNotBeAvailableInTestClassLoader);
            } catch (ClassNotFoundException ignored) {
                // OK
            }

            val relocatedTestLogicClass = classLoader.loadClass(logicClass.getName());
            val relocatedTestLogicCtor = makeAccessible(relocatedTestLogicClass.getDeclaredConstructor());
            val relocatedTestLogic = (ClassesRelocatorTestLogic) relocatedTestLogicCtor.newInstance();
            relocatedTestLogic.assertTestLogic();
        }
    }


    private static final List<Class<?>> TEST_LOGIC_CLASSES = ImmutableList.of(
        ClassesRelocatorTestLogic.class
    );

    @SneakyThrows
    private Path createSourceJar(Class<? extends ClassesRelocatorTestLogic> logicClass) {
        val sourceJarPath = createParentDirectories(tempDirPath.resolve("source.jar"));
        try (
            val testClassesJar = ZipFile.builder().setPath(getTestClassesJarPath()).get();
            val sourceJar = new ZipArchiveOutputStream(sourceJarPath)
        ) {
            sourceJar.setMethod(DEFLATED);
            sourceJar.setLevel(BEST_COMPRESSION);
            sourceJar.setUseZip64(Zip64Mode.AsNeeded);
            sourceJar.setEncoding(UTF_8.name());

            val logicPackageResourceNamePrefix = packageNameOf(logicClass).replace('.', '/') + '/';
            val entriesToCopy = list(testClassesJar.getEntries()).stream()
                .filter(not(ZipArchiveEntry::isDirectory))
                .filter(entry -> {
                    val resourceName = entry.getName();
                    return resourceName.startsWith("META-INF/")
                        || resourceName.startsWith(logicPackageResourceNamePrefix);
                })
                .collect(toImmutableList());

            for (val entryToCopy : entriesToCopy) {
                val sourceEntry = new ZipArchiveEntry(entryToCopy);
                sourceJar.putArchiveEntry(sourceEntry);
                try (val sourceIn = testClassesJar.getInputStream(entryToCopy)) {
                    copy(sourceIn, sourceJar);
                }
                sourceJar.closeArchiveEntry();
            }
        }
        return sourceJarPath;
    }

    private static Path getTestClassesJarPath() {
        val property = "test-classes-jar";
        val path = System.getProperty(property);
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("System property not set: " + property);
        }

        return Paths.get(path);
    }

    private static Collection<Path> getLibraryFilePaths(String libraryName) {
        val classpathString = System.getProperty(libraryName + "-classpath");
        if (classpathString == null) {
            throw new IllegalStateException("Unknown library: " + libraryName);
        }

        return Splitter.on(pathSeparator).splitToStream(classpathString)
            .filter(not(String::isEmpty))
            .map(Paths::get)
            .collect(toImmutableSet());
    }

    private static final Set<String> TEST_LOGIC_CLASS_NAMES = TEST_LOGIC_CLASSES.stream()
        .map(Class::getName)
        .collect(toImmutableSet());

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
