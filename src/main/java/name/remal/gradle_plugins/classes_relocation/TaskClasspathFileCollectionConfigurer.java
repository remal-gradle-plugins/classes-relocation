package name.remal.gradle_plugins.classes_relocation;

import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.val;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

class TaskClasspathFileCollectionConfigurer<T extends Task> extends TaskClasspathConfigurer<T> {

    private final Function<? super T, ? extends FileCollection> getter;

    private final BiConsumer<? super T, ? super FileCollection> setter;

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        Spec<? super T> taskPredicate,
        Function<? super T, ? extends FileCollection> getter,
        BiConsumer<? super T, ? super FileCollection> setter
    ) {
        super(taskType, taskPredicate);
        this.getter = getter;
        this.setter = setter;
    }

    public TaskClasspathFileCollectionConfigurer(
        Class<T> taskType,
        Function<? super T, ? extends FileCollection> getter,
        BiConsumer<? super T, ? super FileCollection> setter
    ) {
        super(taskType);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void configureTask(
        T task,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        val classpath = getter.apply(task);
        val modifiedClasspath = createModifiedClasspath(
            task,
            classpath,
            sourceSetProvider,
            relocateJarProvider
        );
        if (modifiedClasspath != null) {
            modifiedClasspath.builtBy(classpath);
            setter.accept(task, modifiedClasspath);
        }
    }

}