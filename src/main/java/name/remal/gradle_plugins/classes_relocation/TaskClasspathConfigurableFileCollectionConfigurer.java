package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.jspecify.annotations.Nullable;

class TaskClasspathConfigurableFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        @Nullable Spec<T> taskPredicate,
        TaskClasspathConfigurableGetter<T> getter
    ) {
        super(
            taskType,
            taskPredicate,
            getter,
            (task, classpath) -> getter.getClasspath(task).setFrom(classpath)
        );
    }

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        TaskClasspathConfigurableGetter<T> getter
    ) {
        this(
            taskType,
            null,
            getter
        );
    }

}
