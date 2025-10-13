package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jspecify.annotations.Nullable;

class ClasspathElementDir extends ClasspathElementBase {

    public ClasspathElementDir(Path path) {
        super(path);
    }

    @Override
    public boolean isMultiRelease() {
        return false;
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() throws Exception {
        var dirPath = getPath();
        try (var paths = walk(dirPath)) {
            return paths
                .filter(Files::isRegularFile)
                .map(filePath -> dirPath.relativize(filePath).toString())
                .map(ResourceFile::new)
                .collect(toList());
        }
    }


    private class ResourceFile extends ResourceBase {

        private final Path path;
        private final LazyValue<Long> lastModifiedMillis;

        public ResourceFile(String name) {
            super(name);
            this.path = getPath().resolve(name);
            this.lastModifiedMillis = lazyValue(() ->
                getLastModifiedTime(path).toMillis()
            );
        }

        @Nullable
        @Override
        public ClasspathElement getClasspathElement() {
            return ClasspathElementDir.this;
        }

        @Override
        @SneakyThrows
        public long getLastModifiedMillis() {
            return lastModifiedMillis.get();
        }

        @Override
        public InputStream open() throws IOException {
            return newInputStream(path);
        }

    }

}
