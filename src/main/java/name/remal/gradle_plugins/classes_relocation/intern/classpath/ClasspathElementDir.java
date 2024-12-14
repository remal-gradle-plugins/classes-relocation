package name.remal.gradle_plugins.classes_relocation.intern.classpath;

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

        public ResourceFile(String name) {
            super(name);
            this.path = getPath().resolve(name);
        }

        @Nullable
        @Override
        public ClasspathElement getClasspathElement() {
            return ClasspathElementDir.this;
        }

        @Override
        @SneakyThrows
        public long getLastModifiedMillis() {
            return getLastModifiedTime(path).toMillis();
        }

        @Override
        public InputStream open() throws IOException {
            return newInputStream(path);
        }

    }

}
