package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;

import com.google.errorprone.annotations.ForOverride;
import javax.annotation.Nullable;
import lombok.val;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

abstract class TaskClasspathConfigurer<T extends Task> {

    private static final Spec<? super Task> ALWAYS_TRUE_TASK_SPEC = __ -> true;


    @ForOverride
    protected abstract void configureTask(
        T task,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    );


    private final Class<T> taskType;

    private final Spec<? super T> taskPredicate;

    protected TaskClasspathConfigurer(Class<T> taskType, Spec<? super T> taskPredicate) {
        this.taskType = taskType;
        this.taskPredicate = taskPredicate;
    }

    protected TaskClasspathConfigurer(Class<T> taskType) {
        this(taskType, ALWAYS_TRUE_TASK_SPEC);
    }


    public void configureTasks(
        TaskContainer tasks,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        tasks
            .withType(taskType)
            .matching(taskPredicate)
            .configureEach(it -> {
                it.dependsOn(relocateJarProvider);
                doBeforeTaskExecution(it, task ->
                    configureTask(task, sourceSetProvider, relocateJarProvider)
                );
            });
    }

    @Nullable
    protected final ConfigurableFileCollection createModifiedClasspath(
        Task task,
        @Nullable FileCollection classpath,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        if (classpath == null) {
            return null;
        }

        val classpathFiles = classpath.getFiles();
        if (classpathFiles.isEmpty()) {
            return null;
        }

        boolean foundSourceSetOutput = false;
        val modifiedClasspath = task.getProject().getObjects().fileCollection();
        val sourceSetOutputFiles = sourceSetProvider.get().getOutput().getFiles();
        for (val file : classpathFiles) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    modifiedClasspath.from(relocateJarProvider.flatMap(RelocateJar::getTargetJarFile))
                        .builtBy(relocateJarProvider);
                    foundSourceSetOutput = true;
                }
            } else {
                modifiedClasspath.from(file);
            }
        }

        if (foundSourceSetOutput) {
            return modifiedClasspath;
        }

        return null;
    }

}
