package name.remal.gradle_plugins.classes_relocation.intern;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Unmodifiable;

interface RelocationOutput extends Closeable {

    @Unmodifiable
    Set<String> getAddedResourceNames();

    void write(
        String resourceName,
        @Nullable Long lastModifiedMillis,
        byte[] bytes
    );

    void copy(
        String resourceName,
        @Nullable Long lastModifiedMillis,
        InputStream inputStream
    );

}
