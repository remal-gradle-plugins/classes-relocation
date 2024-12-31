package name.remal.gradle_plugins.classes_relocation;

import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.val;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

class TaskClasspathConfigurableFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    private final Function<? super T, ? extends ConfigurableFileCollection> getter;

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        Spec<? super T> taskPredicate,
        Function<? super T, ? extends ConfigurableFileCollection> getter
    ) {
        super(taskType, taskPredicate);
        this.getter = getter;
    }

    public TaskClasspathConfigurableFileCollectionConfigurer(
        Class<T> taskType,
        Function<? super T, ? extends ConfigurableFileCollection> getter
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
    protected void configureTask(
        T task,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
    ) {
        val classpath = getter.apply(task);
        val modifiedClasspath = createModifiedClasspath(
            task,
            classpath,
            sourceSetProvider,
            jarProvider
        );
        if (modifiedClasspath != null) {
            classpath.setFrom(modifiedClasspath);
        }
    }

}
