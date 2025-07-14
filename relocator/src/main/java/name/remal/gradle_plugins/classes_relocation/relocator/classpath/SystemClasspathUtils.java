package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.lang.String.format;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.list;
import static java.util.stream.Collectors.toUnmodifiableList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = PRIVATE)
public abstract class SystemClasspathUtils {

    @SneakyThrows
    public static Classpath getSystemClasspath(Path jvmInstallationDir) {
        if (!exists(jvmInstallationDir)) {
            throw new IllegalStateException(format(
                "JVM installation dir does NOT exist: %s",
                jvmInstallationDir
            ));
        }

        var jrtFsJarPath = jvmInstallationDir.resolve("lib/jrt-fs.jar");
        if (isRegularFile(jrtFsJarPath)) {
            @SuppressWarnings("java:S2095")
            var fileSystem = newFileSystem(
                URI.create("jrt:/"),
                Map.of(
                    "java.home", jvmInstallationDir.toAbsolutePath().toString()
                )
            );
            var modulesPath = fileSystem.getPath("/modules");
            try (var stream = list(modulesPath)) {
                var paths = stream.collect(toUnmodifiableList());
                if (!paths.isEmpty()) {
                    return new ClasspathPaths(fileSystem, paths);
                }
            }
        }

        /*
        var jmodsDirPath = jvmInstallationDir.resolve("jmods");
        if (isDirectory(jmodsDirPath)) {
            try (var stream = list(jmodsDirPath)) {
                var paths = stream
                    .filter(path -> path.getFileName().toString().endsWith(".jmod"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(toUnmodifiableList());
                if (!paths.isEmpty()) {
                    return newClasspathForPaths(paths);
                }
            }
        }
        */

        var jreLibDirPath = jvmInstallationDir.resolve("jre/lib");
        if (isDirectory(jreLibDirPath)) {
            try (var stream = list(jreLibDirPath)) {
                var paths = stream
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(toUnmodifiableList());
                if (!paths.isEmpty()) {
                    return newClasspathForPaths(paths);
                }
            }
        }

        throw new IllegalStateException(format(
            "Unsupported JVM installation dir (no classpath elements found): %s",
            jvmInstallationDir
        ));
    }

    public static Classpath getCurrentSystemClasspath() {
        var javaHome = System.getProperty("java.home");
        if (isEmpty(javaHome)) {
            throw new IllegalStateException("System property `java.home` is not set or empty");
        }
        return getSystemClasspath(Paths.get(javaHome));
    }

}
