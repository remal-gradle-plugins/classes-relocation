package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
interface TaskClasspathConfigurableGetter<T extends Task> extends TaskClasspathGetter<T> {

    @Override
    @NonNull
    ConfigurableFileCollection getClasspath(T task) throws Throwable;

}
