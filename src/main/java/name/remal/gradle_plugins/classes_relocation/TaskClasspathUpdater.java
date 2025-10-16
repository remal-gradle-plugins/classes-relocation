package name.remal.gradle_plugins.classes_relocation;

import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.finalizeFileCollectionValueOnRead;

import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class TaskClasspathUpdater implements Action<Task>, Describable {

    public abstract Property<TaskClasspathGetter<Task>> getGetter();

    public abstract ConfigurableFileCollection getSourceSetOutput();

    public abstract RegularFileProperty getJarFile();

    public abstract Property<TaskClasspathSetter<Task>> getSetter();

    @Override
    @SneakyThrows
    public void execute(Task task) {
        var initialClasspath = getGetter().get().getClasspath(task);
        if (initialClasspath == null) {
            return;
        }

        var initialClasspathFiles = initialClasspath.getFiles();
        if (initialClasspathFiles.isEmpty()) {
            return;
        }
        var sourceSetOutputFiles = getSourceSetOutput().getFiles();
        if (sourceSetOutputFiles.isEmpty()) {
            return;
        }

        boolean foundSourceSetOutput = false;
        var modifiedClasspath = getObjects().fileCollection();
        for (var file : initialClasspathFiles) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    modifiedClasspath.from(getJarFile());
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

                logger.debug("Modified classpath:");
                modifiedClasspath.getFiles().forEach(file -> logger.debug("  {}", file));
            }

            getSetter().get().setClasspath(task, modifiedClasspath);
        }
    }


    @Override
    public String getDisplayName() {
        return TaskClasspathUpdater.class.getSimpleName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
