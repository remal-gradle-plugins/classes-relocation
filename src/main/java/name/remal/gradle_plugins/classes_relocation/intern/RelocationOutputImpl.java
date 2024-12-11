package name.remal.gradle_plugins.classes_relocation.intern;

import static java.util.Collections.unmodifiableSet;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static name.remal.gradle_plugins.classes_relocation.intern.InternalGradleConstantTimeForZipEntries.getInternalGradleConstantTimeForZipEntriesMillis;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.DEFLATED;

import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jetbrains.annotations.UnmodifiableView;

class RelocationOutputImpl implements RelocationOutput {

    private static final long CONSTANT_TIME_FOR_ZIP_ENTRIES =
        getInternalGradleConstantTimeForZipEntriesMillis(318211200000L);


    @GuardedBy("this")
    private final Set<String> addedResourceNames = new LinkedHashSet<>();

    @GuardedBy("this")
    private final ZipArchiveOutputStream zipOutputStream;

    private final boolean preserveFileTimestamps;

    @SneakyThrows
    public RelocationOutputImpl(
        Path targetJarPath,
        Charset metadataCharset,
        boolean preserveFileTimestamps
    ) {
        this.zipOutputStream = new ZipArchiveOutputStream(createParentDirectories(targetJarPath));
        this.zipOutputStream.setMethod(DEFLATED);
        this.zipOutputStream.setLevel(BEST_COMPRESSION);
        this.zipOutputStream.setUseZip64(Zip64Mode.AsNeeded);
        this.zipOutputStream.setEncoding(metadataCharset.name());

        this.preserveFileTimestamps = preserveFileTimestamps;
    }

    private synchronized void checkPath(String path) {
        if (!addedResourceNames.add(path)) {
            throw new IllegalStateException("Duplicate path: " + path);
        }
    }

    private long canonizeLastModified(@Nullable Long lastModifiedMillis) {
        if (lastModifiedMillis == null
            || lastModifiedMillis < CONSTANT_TIME_FOR_ZIP_ENTRIES
            || !preserveFileTimestamps
        ) {
            return CONSTANT_TIME_FOR_ZIP_ENTRIES;
        }

        return lastModifiedMillis;
    }

    @Override
    @UnmodifiableView
    public synchronized Set<String> getAddedResourceNames() {
        return unmodifiableSet(addedResourceNames);
    }

    @Override
    @SneakyThrows
    public synchronized void write(
        String path,
        @Nullable Long lastModifiedMillis,
        byte[] bytes
    ) {
        checkPath(path);
        val archiveEntry = new ZipArchiveEntry(path);
        archiveEntry.setTime(canonizeLastModified(lastModifiedMillis));
        zipOutputStream.putArchiveEntry(archiveEntry);
        zipOutputStream.write(bytes);
        zipOutputStream.closeArchiveEntry();
    }

    @Override
    @SneakyThrows
    public synchronized void copy(
        String path,
        @Nullable Long lastModifiedMillis,
        @WillNotClose InputStream inputStream
    ) {
        checkPath(path);
        val archiveEntry = new ZipArchiveEntry(path);
        archiveEntry.setTime(canonizeLastModified(lastModifiedMillis));
        zipOutputStream.putArchiveEntry(archiveEntry);
        ByteStreams.copy(inputStream, zipOutputStream);
        zipOutputStream.closeArchiveEntry();
    }

    @Override
    public synchronized void close() throws IOException {
        zipOutputStream.close();
    }

}
