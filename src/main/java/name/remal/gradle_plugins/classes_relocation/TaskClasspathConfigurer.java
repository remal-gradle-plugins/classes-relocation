package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.BuildFeaturesUtils.areIsolatedProjectsRequested;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.finalizeFileCollectionValueOnRead;
import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;

import com.google.errorprone.annotations.ForOverride;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jspecify.annotations.Nullable;

abstract class TaskClasspathConfigurer<T extends Task> {

    @ForOverride
    @Nullable
    protected abstract FileCollection getClasspath(T task);

    @ForOverride
    protected abstract void setClasspath(T task, FileCollection classpath);


    private final Class<T> taskType;
    private final Spec<? super T> taskPredicate;

    protected TaskClasspathConfigurer(Class<T> taskType, Spec<? super T> taskPredicate) {
        this.taskType = taskType;
        this.taskPredicate = taskPredicate;
    }

    private static final Spec<? super Task> ALWAYS_TRUE_TASK_SPEC = __ -> true;

    protected TaskClasspathConfigurer(Class<T> taskType) {
        this(taskType, ALWAYS_TRUE_TASK_SPEC);
    }


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

        var jarFileProperty = objects.fileProperty().value(jarProvider.flatMap(Jar::getArchiveFile));
        jarFileProperty.finalizeValueOnRead();

        Action<Project> configureProject = currentProject -> {
            currentProject.getTasks()
                .withType(taskType)
                .matching(taskPredicate)
                .configureEach(task -> {
                    task.dependsOn(providers.provider(() ->
                        calculateDependsOn(task, sourceSetOutput, jarProvider)
                    ));

                    var initialClasspath = objects.fileCollection()
                        .from(providers.provider(() -> getClasspath(task)));
                    finalizeFileCollectionValueOnRead(initialClasspath);
                    task.dependsOn(initialClasspath);

                    var modifiedClasspath = objects.fileCollection();

                    doBeforeTaskExecution(task, currentTask ->
                        updateClasspathIfNeeded(
                            currentTask,
                            initialClasspath,
                            sourceSetOutput,
                            jarFileProperty,
                            modifiedClasspath
                        )
                    );
                });
        };
        if (areIsolatedProjectsRequested(project.getGradle())) {
            configureProject.execute(project);
        } else {
            project.getRootProject().allprojects(configureProject);
        }
    }

    private Object calculateDependsOn(
        T task,
        FileCollection sourceSetOutput,
        TaskProvider<Jar> jarProvider
    ) {
        var classpath = getClasspath(task);
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
            return jarProvider;
        }

        return List.of();
    }

    private void updateClasspathIfNeeded(
        T task,
        FileCollection initialClasspath,
        FileCollection sourceSetOutput,
        RegularFileProperty jarFileProperty,
        ConfigurableFileCollection modifiedClasspath
    ) {
        var initialClasspathFiles = initialClasspath.getFiles();
        var sourceSetOutputFiles = sourceSetOutput.getFiles();
        boolean foundSourceSetOutput = false;
        for (var file : initialClasspathFiles) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    modifiedClasspath.from(jarFileProperty);
                    foundSourceSetOutput = true;
                }
            } else {
                modifiedClasspath.from(file);
            }
        }
        finalizeFileCollectionValueOnRead(modifiedClasspath);

        if (foundSourceSetOutput) {
            var logger = task.getLogger();
            if (logger.isDebugEnabled()) {
                logger.debug("Initial classpath:");
                initialClasspathFiles.forEach(file -> logger.debug("  {}", file));

                logger.debug("Modified classpath ({}):", getClass().getSimpleName());
                modifiedClasspath.getFiles().forEach(file -> logger.debug("  {}", file));
            }

            setClasspath(task, modifiedClasspath);
        }
    }

}
