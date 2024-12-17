package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import java.io.Closeable;
import java.nio.file.Path;

public interface ClasspathElement extends WithResources, Closeable {

    Path getPath();

    String getModuleName();

    boolean isMultiRelease();

}
