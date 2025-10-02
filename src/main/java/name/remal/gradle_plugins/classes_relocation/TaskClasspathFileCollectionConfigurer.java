package name.remal.gradle_plugins.classes_relocation;

import java.util.function.BiConsumer;
import java.util.function.Function;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.jspecify.annotations.Nullable;

class TaskClasspathFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    private final Function<? super T, ? extends @Nullable FileCollection> getter;
    private final BiConsumer<? super T, ? super FileCollection> setter;

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        Spec<? super T> taskPredicate,
        Function<? super T, ? extends @Nullable FileCollection> getter,
        BiConsumer<? super T, ? super FileCollection> setter
    ) {
        super(taskType, taskPredicate);
        this.getter = getter;
        this.setter = setter;
    }

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        Function<? super T, ? extends @Nullable FileCollection> getter,
        BiConsumer<? super T, ? super FileCollection> setter
    ) {
        super(taskType);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    @Nullable
    protected FileCollection getClasspath(T task) {
        return getter.apply(task);
    }

    @Override
    protected void setClasspath(T task, FileCollection classpath) {
        setter.accept(task, classpath);
    }

}
