package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.nio.file.Files.isDirectory;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = PRIVATE)
public abstract class SystemClasspathUtils {

    public static List<Path> getSystemClasspathPaths() {
        var javaHome = System.getProperty("java.home");
        if (isEmpty(javaHome)) {
            throw new IllegalStateException("System property `java_home` is not set or empty");
        }
        return getSystemClasspathPaths(Paths.get(javaHome));
    }

    @SneakyThrows
    public static List<Path> getSystemClasspathPaths(Path jvmInstallationDir) {
        var paths = new ArrayList<Path>();

        var jreLibDirPath = jvmInstallationDir.resolve("jre/lib");
        if (isDirectory(jreLibDirPath)) {
            try (var stream = Files.list(jreLibDirPath)) {
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(paths::add);
            }
        }

        var jmodsDirPath = jvmInstallationDir.resolve("jmods");
        if (isDirectory(jmodsDirPath)) {
            try (var stream = Files.list(jmodsDirPath)) {
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".jmod"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(paths::add);
            }
        }

        return List.copyOf(paths);
    }

    public static List<Path> getSystemClasspathPaths(File jvmInstallationDir) {
        return getSystemClasspathPaths(jvmInstallationDir.toPath());
    }

}
