package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.list;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.Unmodifiable;

@RequiredArgsConstructor
class ClasspathElementJar extends ClasspathElementBase {

    private final Path zipFilePath;


    @Nullable
    @Override
    public Path getPath() {
        return zipFilePath;
    }


    @Getter(lazy = true)
    private final ZipFile zipFile = openZipFile();

    @SneakyThrows
    private ZipFile openZipFile() {
        return registerCloseable(
            ZipFile.builder()
                .setPath(zipFilePath)
                .get()
        );
    }


    @Unmodifiable
    @Getter
    private final List<Resource> resources = asLazyListProxy(LazyValue.of(() -> {
        val entries = list(getZipFile().getEntries());
        return entries.stream()
            .sorted(comparing(ZipArchiveEntry::getName))
            .map(ResourceJar::new)
            .collect(toImmutableList());
    }));

    @RequiredArgsConstructor
    private class ResourceJar extends ResourceBase {

        private final ZipArchiveEntry entry;

        @Override
        public ClasspathElement getClasspathElement() {
            return ClasspathElementJar.this;
        }

        @Override
        public String getPath() {
            return entry.getName();
        }

        @Getter(lazy = true)
        private final long lastModifiedMillis = readLastModifiedMillis();

        @SneakyThrows
        private long readLastModifiedMillis() {
            return requireNonNull(entry).getTime();
        }

        @Override
        public InputStream open() throws IOException {
            return getZipFile().getInputStream(entry);
        }

        @Override
        public String toString() {
            return zipFilePath.toString() + '[' + getPath() + ']';
        }

    }

    @Override
    public String toString() {
        return zipFilePath.toString();
    }

}
