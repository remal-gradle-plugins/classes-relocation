package name.remal.gradle_plugins.classes_relocation;

import java.util.function.Function;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.jspecify.annotations.Nullable;

class TaskClasspathConfigurableFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    private final Function<? super T, ? extends ConfigurableFileCollection> getter;

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        Spec<? super T> taskPredicate,
        Function<? super T, ? extends @Nullable ConfigurableFileCollection> getter
    ) {
        super(taskType, taskPredicate);
        this.getter = getter;
    }

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        Function<? super T, ? extends @Nullable ConfigurableFileCollection> getter
    ) {
        super(taskType);
        this.getter = getter;
    }

    @Override
    @Nullable
    protected FileCollection getClasspath(T task) {
        return getter.apply(task);
    }

    @Override
    protected void setClasspath(T task, FileCollection classpath) {
        getter.apply(task).setFrom(classpath);
    }

}
