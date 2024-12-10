package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walk;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;

@RequiredArgsConstructor
class ClasspathElementDir extends ClasspathElementBase {

    private final Path dirPath;


    @Nullable
    @Override
    public Path getPath() {
        return dirPath;
    }


    @Unmodifiable
    @Getter
    private final List<Resource> resources = asLazyListProxy(LazyValue.of(() -> {
        try (val stream = walk(requireNonNull(dirPath))) {
            return stream
                .filter(Files::isRegularFile)
                .sorted()
                .map(ResourceDir::new)
                .collect(toImmutableList());
        }
    }));

    @RequiredArgsConstructor
    private class ResourceDir extends ResourceBase {

        private final Path filePath;

        @Override
        public ClasspathElement getClasspathElement() {
            return ClasspathElementDir.this;
        }

        @Getter(lazy = true)
        private final String path = calculateRelativePath();

        private String calculateRelativePath() {
            String relativePath = requireNonNull(filePath).relativize(dirPath).toString();
            if (File.separatorChar != '/') {
                relativePath = relativePath.replace(File.separatorChar, '/');
            }
            return relativePath;
        }

        @Getter(lazy = true)
        private final long lastModifiedMillis = readLastModifiedMillis();

        @SneakyThrows
        private long readLastModifiedMillis() {
            return getLastModifiedTime(requireNonNull(filePath)).toMillis();
        }

        @Override
        public InputStream open() throws IOException {
            return newInputStream(filePath);
        }

        @Override
        public String toString() {
            return dirPath.toString() + '[' + getPath() + ']';
        }

    }


    @Override
    public String toString() {
        return dirPath.toString();
    }

}
