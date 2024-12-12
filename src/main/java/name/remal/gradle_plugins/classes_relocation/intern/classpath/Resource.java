package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.io.ByteStreams.toByteArray;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.val;

public interface Resource {

    @Nullable
    ClasspathElement getClasspathElement();

    String getName();

    long getLastModifiedMillis();

    @MustBeClosed
    InputStream open() throws IOException;

    default byte[] readAllBytes() throws IOException {
        try (val in = open()) {
            return toByteArray(in);
        }
    }

}
