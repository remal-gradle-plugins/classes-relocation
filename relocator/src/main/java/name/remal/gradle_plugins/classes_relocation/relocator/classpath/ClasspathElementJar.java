package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.lang.Boolean.parseBoolean;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.list;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.MULTI_RELEASE;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.jar.Manifest;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

class ClasspathElementJar extends ClasspathElementBase {

    private static final long MAX_FILE_SIZE_TO_LOAD_IN_HEAP_BYTES = 1024 * 1024L;


    private final LazyValue<ZipFile> zipFile;
    private final LazyValue<Boolean> multiRelease;

    public ClasspathElementJar(Path path) {
        super(path);
        this.zipFile = lazyValue(() -> {
            val zipFileBuilder = ZipFile.builder().setPath(path);
            val fileSize = Files.size(path);
            if (fileSize <= MAX_FILE_SIZE_TO_LOAD_IN_HEAP_BYTES) {
                val bytes = readAllBytes(path);
                zipFileBuilder.setByteArray(bytes);
            }
            return closables.registerCloseable(zipFileBuilder.get());
        });
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

            return parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE));
        });
    }

    @Override
    public boolean isMultiRelease() {
        return multiRelease.get();
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() {
        val processedEntryNames = new LinkedHashSet<>();
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
