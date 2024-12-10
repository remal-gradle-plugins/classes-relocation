package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.list;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.MULTI_RELEASE_NAME;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.jar.Manifest;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

class ClasspathElementJar extends ClasspathElementBase {

    private final LazyValue<ZipFile> zipFile;
    private final LazyValue<Boolean> multiRelease;

    public ClasspathElementJar(Path path) {
        super(path);
        this.zipFile = lazyValue(() -> closables.registerCloseable(
            ZipFile.builder()
                .setPath(path)
                .get()
        ));
        this.multiRelease = lazyValue(() -> {
            val zipFile = this.zipFile.get();
            val manifestEntry = zipFile.getEntry(MANIFEST_NAME);
            if (manifestEntry == null) {
                return false;
            }

            val manifest = new Manifest();
            try (val in = zipFile.getInputStream(manifestEntry)) {
                manifest.read(in);
            }

            return parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE_NAME));
        });
    }

    @Override
    public boolean isMultiRelease() {
        return multiRelease.get();
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() {
        return list(zipFile.get().getEntries()).stream()
            .map(ResourceJar::new)
            .collect(toList());
    }


    private class ResourceJar extends ResourceBase {

        private final ZipArchiveEntry zipEntry;

        public ResourceJar(
            ZipArchiveEntry zipEntry
        ) {
            super(zipEntry.getName());
            this.zipEntry = zipEntry;
        }

        @Nullable
        @Override
        public ClasspathElement getElement() {
            return ClasspathElementJar.this;
        }

        @Override
        public long getLastModifiedMillis() {
            return zipEntry.getTime();
        }

        @Override
        public InputStream open() throws IOException {
            return zipFile.get().getInputStream(zipEntry);
        }

        @Override
        public String toString() {
            return getPath().toString() + '[' + getName() + ']';
        }

    }

}
