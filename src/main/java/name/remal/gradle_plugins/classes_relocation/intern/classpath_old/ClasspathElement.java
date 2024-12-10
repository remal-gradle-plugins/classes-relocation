package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import java.io.Closeable;
import java.nio.file.Path;
import javax.annotation.Nullable;

public interface ClasspathElement extends WithResources, Closeable {

    @Nullable
    Path getPath();

    String getModuleName();

}
