package name.remal.gradle_plugins.classes_relocation;

import static java.util.Collections.emptyList;
import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;

import com.google.errorprone.annotations.ForOverride;
import javax.annotation.Nullable;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

abstract class TaskClasspathConfigurer<T extends Task> {

    private static final Spec<? super Task> ALWAYS_TRUE_TASK_SPEC = __ -> true;


    @ForOverride
    @Nullable
    protected abstract FileCollection getClasspath(T task);

    @ForOverride
    protected abstract void configureTask(
        T task,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
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
        Project project,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
    ) {
        project.getRootProject().allprojects(currentProject -> {
            currentProject.getTasks()
                .withType(taskType)
                .matching(taskPredicate)
                .configureEach(task -> {
                    task.dependsOn(task.getProject().getProviders().provider(() ->
                        calculateDependsOn(task, sourceSetProvider, jarProvider)
                    ));

                    doBeforeTaskExecution(task, it ->
                        configureTask(it, sourceSetProvider, jarProvider)
                    );
                });
        });
    }

    private Object calculateDependsOn(
        T task,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
    ) {
        var classpath = getClasspath(task);
        if (classpath == null) {
            return emptyList();
        }

        var classpathFiles = classpath.getFiles();
        if (classpathFiles.isEmpty()) {
            return emptyList();
        }

        var sourceSetOutputFiles = sourceSetProvider.get().getOutput().getFiles();
        var hasSourceSetOutputFiles = classpathFiles.stream()
            .anyMatch(sourceSetOutputFiles::contains);
        if (hasSourceSetOutputFiles) {
            return jarProvider;
        }

        return emptyList();
    }

    @Nullable
    protected final ConfigurableFileCollection createModifiedClasspath(
        Task task,
        @Nullable FileCollection classpath,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
    ) {
        if (classpath == null) {
            return null;
        }

        var classpathFiles = classpath.getFiles();
        if (classpathFiles.isEmpty()) {
            return null;
        }

        boolean foundSourceSetOutput = false;
        var modifiedClasspath = task.getProject().getObjects().fileCollection();
        var sourceSetOutputFiles = sourceSetProvider.get().getOutput().getFiles();
        for (var file : classpathFiles) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    modifiedClasspath
                        .builtBy(jarProvider)
                        .from(jarProvider.flatMap(Jar::getArchiveFile));
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
