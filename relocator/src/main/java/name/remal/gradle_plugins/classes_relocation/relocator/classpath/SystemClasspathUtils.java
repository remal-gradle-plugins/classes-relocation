package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.nio.file.Files.isDirectory;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public abstract class SystemClasspathUtils {

    public static List<Path> getSystemClasspathPaths() {
        val javaHome = System.getProperty("java.home");
        if (isEmpty(javaHome)) {
            throw new IllegalStateException("System property `java_home` is not set or empty");
        }
        return getSystemClasspathPaths(Paths.get(javaHome));
    }

    @SneakyThrows
    public static List<Path> getSystemClasspathPaths(Path jvmInstallationDir) {
        val paths = ImmutableList.<Path>builder();

        val jreLibDirPath = jvmInstallationDir.resolve("jre/lib");
        if (isDirectory(jreLibDirPath)) {
            try (val stream = Files.list(jreLibDirPath)) {
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(paths::add);
            }
        }

        val jmodsDirPath = jvmInstallationDir.resolve("jmods");
        if (isDirectory(jmodsDirPath)) {
            try (val stream = Files.list(jmodsDirPath)) {
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".jmod"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(paths::add);
            }
        }

        return paths.build();
    }

    public static List<Path> getSystemClasspathPaths(File jvmInstallationDir) {
        return getSystemClasspathPaths(jvmInstallationDir.toPath());
    }

}
