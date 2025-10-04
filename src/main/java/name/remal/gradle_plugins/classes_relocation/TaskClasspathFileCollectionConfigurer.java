package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.jspecify.annotations.Nullable;

class TaskClasspathFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        @Nullable Spec<T> taskPredicate,
        TaskClasspathGetter<T> getter,
        TaskClasspathSetter<T> setter
    ) {
        super(
            taskType,
            taskPredicate,
            getter,
            setter
        );
    }

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        TaskClasspathGetter<T> getter,
        TaskClasspathSetter<T> setter
    ) {
        this(
            taskType,
            null,
            getter,
            setter
        );
    }

}
