package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.lang.Boolean.parseBoolean;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.list;
import static java.util.function.Predicate.not;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathUtils.MAX_ARCHIVE_FILE_SIZE_TO_LOAD_IN_HEAP_BYTES;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.MULTI_RELEASE;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.jar.Manifest;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

class ClasspathElementJar extends ClasspathElementBase {

    private final LazyValue<ZipFile> zipFile;
    private final LazyValue<Boolean> multiRelease;

    public ClasspathElementJar(Path path) {
        super(path);
        this.zipFile = lazyValue(() -> {
            var zipFileBuilder = ZipFile.builder().setPath(path);
            var fileSize = Files.size(path);
            if (fileSize <= MAX_ARCHIVE_FILE_SIZE_TO_LOAD_IN_HEAP_BYTES) {
                var bytes = readAllBytes(path);
                zipFileBuilder.setByteArray(bytes);
            }
            return closables.registerCloseable(zipFileBuilder.get());
        });
        this.multiRelease = lazyValue(() -> {
            var zipFile = this.zipFile.get();
            var manifestEntry = zipFile.getEntry(MANIFEST_NAME);
            if (manifestEntry == null) {
                return false;
            }

            var manifest = new Manifest();
            try (var in = zipFile.getInputStream(manifestEntry)) {
                manifest.read(in);
            }

            return parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE));
        });
    }

    @Override
    public boolean isMultiRelease() {
        return multiRelease.get();
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() {
        var processedEntryNames = new LinkedHashSet<>();
        return list(zipFile.get().getEntries()).stream()
            .filter(not(ZipArchiveEntry::isDirectory))
            .filter(entry -> processedEntryNames.add(entry.getName()))
            .map(ResourceJar::new)
            .collect(toList());
    }


    private class ResourceJar extends ResourceBase {

        private final ZipArchiveEntry zipEntry;

        public ResourceJar(ZipArchiveEntry zipEntry) {
            super(zipEntry.getName());
            this.zipEntry = zipEntry;
        }

        @Nullable
        @Override
        public ClasspathElement getClasspathElement() {
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

    }

}
