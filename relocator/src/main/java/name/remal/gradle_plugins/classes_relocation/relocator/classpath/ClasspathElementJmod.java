package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.Collections.list;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

class ClasspathElementJmod extends ClasspathElementBase {

    private static final String CLASSES_RESOURCE_NAME_PREFIX = "classes/";


    private final LazyValue<ZipFile> zipFile;

    public ClasspathElementJmod(Path path) {
        super(path);
        this.zipFile = lazyValue(() -> {
            var zipFileBuilder = ZipFile.builder().setPath(path);
            return closeables.registerCloseable(zipFileBuilder.get());
        });
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() {
        var processedEntryNames = new LinkedHashSet<>();
        return list(zipFile.get().getEntries()).stream()
            .filter(not(ZipArchiveEntry::isDirectory))
            .filter(entry -> entry.getName().startsWith(CLASSES_RESOURCE_NAME_PREFIX))
            .filter(entry -> processedEntryNames.add(entry.getName()))
            .map(entry -> new ResourceJmod(
                entry.getName().substring(CLASSES_RESOURCE_NAME_PREFIX.length()),
                entry
            ))
            .collect(toList());
    }

    @Override
    public boolean isMultiRelease() {
        return false;
    }


    private class ResourceJmod extends ResourceBase {

        private final ZipArchiveEntry zipEntry;

        public ResourceJmod(String name, ZipArchiveEntry zipEntry) {
            super(name);
            this.zipEntry = zipEntry;
        }

        @Nullable
        @Override
        public ClasspathElement getClasspathElement() {
            return ClasspathElementJmod.this;
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
        protected String getToStringNamePrefix() {
            return CLASSES_RESOURCE_NAME_PREFIX;
        }

    }

}
