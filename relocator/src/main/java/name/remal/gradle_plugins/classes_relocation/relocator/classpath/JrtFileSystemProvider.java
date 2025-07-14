package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.nio.file.FileSystems.newFileSystem;
import static lombok.AccessLevel.PRIVATE;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = PRIVATE)
abstract class JrtFileSystemProvider {

    @SneakyThrows
    public static FileSystem newJrtFileSystem(Path jvmInstallationDir) {
        return newFileSystem(
            URI.create("jrt:/"),
            Map.of(
                "java.home", jvmInstallationDir.toAbsolutePath().toString()
            )
        );
    }

}
