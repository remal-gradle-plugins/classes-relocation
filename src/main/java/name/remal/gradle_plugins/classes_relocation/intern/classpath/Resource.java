package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public interface Resource {

    @Nullable
    ClasspathElement getClasspathElement();

    String getName();

    long getLastModifiedMillis();

    @MustBeClosed
    InputStream open() throws IOException;

}
