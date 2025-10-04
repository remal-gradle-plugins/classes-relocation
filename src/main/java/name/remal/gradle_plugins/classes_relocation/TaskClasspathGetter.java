package name.remal.gradle_plugins.classes_relocation;

import java.io.Serializable;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
interface TaskClasspathGetter<T extends Task> extends Serializable {

    @Nullable
    FileCollection getClasspath(T task) throws Throwable;

}
