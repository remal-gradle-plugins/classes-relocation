package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.io.InputStream;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.content.Content;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationDestination;

public interface Resource {

    ClasspathElement getClasspathElement();

    String getPath();

    long getLastModifiedMillis();

    @MustBeClosed
    InputStream open() throws IOException;

    @CheckReturnValue
    Content getContent();

    @SneakyThrows
    default void writeTo(RelocationDestination destination) {
        try (val inputStream = open()) {
            destination.copy(inputStream);
        }
    }

}
