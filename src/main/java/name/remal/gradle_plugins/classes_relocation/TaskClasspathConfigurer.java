package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.BuildFeaturesUtils.areIsolatedProjectsRequested;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.finalizeFileCollectionValueOnRead;

import java.util.Collection;
import java.util.List;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jspecify.annotations.Nullable;

abstract class TaskClasspathConfigurer<T extends Task> {

    private final Class<T> taskType;
    private final Spec<T> taskPredicate;
    private final TaskClasspathGetter<T> getter;
    private final TaskClasspathSetter<T> setter;

    protected TaskClasspathConfigurer(
        Class<T> taskType,
        @Nullable Spec<T> taskPredicate,
        TaskClasspathGetter<T> getter,
        TaskClasspathSetter<T> setter
    ) {
        this.taskType = taskType;
        this.taskPredicate = taskPredicate != null ? taskPredicate : __ -> true;
        this.getter = getter;
        this.setter = setter;
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void configureTasks(
        Project project,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<Jar> jarProvider
    ) {
        var providers = project.getProviders();
        var objects = project.getObjects();

        var sourceSetOutput = objects.fileCollection()
            .from(sourceSetProvider.map(SourceSet::getOutput));
        finalizeFileCollectionValueOnRead(sourceSetOutput);

        var jarFile = objects.fileProperty().value(jarProvider.flatMap(Jar::getArchiveFile));
        jarFile.finalizeValueOnRead();

        Action<Project> configureProject = currentProject -> {
            currentProject.getTasks()
                .withType(taskType)
                .matching(taskPredicate)
                .configureEach(task -> {
                    task.dependsOn(providers.provider(() ->
                        calculateDependsOn(task, sourceSetOutput, jarProvider)
                    ));

                    currentProject.getLogger().debug("Registering relocation classpath updater for task {}", task);
                    var classpathUpdater = objects.newInstance(TaskClasspathUpdater.class);
                    classpathUpdater.getGetter().set((TaskClasspathGetter) getter);
                    classpathUpdater.getSourceSetOutput().from(sourceSetOutput);
                    classpathUpdater.getJarFile().set(jarFile);
                    classpathUpdater.getSetter().set((TaskClasspathSetter) setter);
                    task.onlyIf(classpathUpdater);
                });
        };
        if (areIsolatedProjectsRequested(project.getGradle())) {
            configureProject.execute(project);
        } else {
            project.getRootProject().allprojects(configureProject);
        }
    }

    @SneakyThrows
    private Collection<Object> calculateDependsOn(
        T task,
        FileCollection sourceSetOutput,
        TaskProvider<Jar> jarProvider
    ) {
        var classpath = getter.getClasspath(task);
        if (classpath == null) {
            return List.of();
        }

        var classpathFiles = classpath.getFiles();
        if (classpathFiles.isEmpty()) {
            return List.of();
        }

        var sourceSetOutputFiles = sourceSetOutput.getFiles();
        var hasSourceSetOutputFiles = classpathFiles.stream()
            .anyMatch(sourceSetOutputFiles::contains);
        if (hasSourceSetOutputFiles) {
            return List.of(jarProvider);
        }

        return List.of();
    }

}
