package name.remal.gradle_plugins.classes_relocation.relocator;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Set;
import javax.annotation.WillNotClose;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

interface RelocationOutput extends Closeable {

    @Unmodifiable
    Set<String> getAddedResourceNames();

    default boolean isResourceAdded(String resourceName) {
        return getAddedResourceNames().contains(resourceName);
    }

    void write(
        String resourceName,
        @Nullable Long lastModifiedMillis,
        byte[] bytes
    );

    void copy(
        String resourceName,
        @Nullable Long lastModifiedMillis,
        @WillNotClose InputStream inputStream
    );

}
