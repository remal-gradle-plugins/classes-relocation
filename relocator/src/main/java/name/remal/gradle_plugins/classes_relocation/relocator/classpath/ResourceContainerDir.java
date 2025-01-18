package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;

class ResourceContainerDir extends ResourceContainerBase {

    public ResourceContainerDir(Path path) {
        super(path);
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() throws Exception {
        val dirPath = getPath();
        try (val paths = walk(dirPath)) {
            return paths
                .filter(Files::isRegularFile)
                .map(filePath -> filePath.relativize(dirPath).toString())
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
            this.lastModifiedMillis = LazyValue.lazyValue(() ->
                getLastModifiedTime(path).toMillis()
            );
        }

        @Nullable
        @Override
        public ClasspathElement getClasspathElement() {
            return null;
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
