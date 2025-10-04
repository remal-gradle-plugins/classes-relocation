package name.remal.gradle_plugins.classes_relocation;

import java.io.Serializable;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

@FunctionalInterface
interface TaskClasspathSetter<T extends Task> extends Serializable {

    void setClasspath(T task, FileCollection classpath) throws Throwable;

}
