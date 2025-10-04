package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.finalizeFileCollectionValueOnRead;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;

@RequiredArgsConstructor
abstract class TaskClasspathUpdater implements Spec<Task>, Describable {

    public abstract ConfigurableFileCollection getInitialClasspath();

    public abstract ConfigurableFileCollection getSourceSetOutput();

    public abstract RegularFileProperty getJarFile();

    public abstract Property<TaskClasspathSetter<Task>> getSetter();

    @Override
    @SneakyThrows
    public boolean isSatisfiedBy(Task task) {
        var modifiedClasspath = getObjects().fileCollection();

        var initialClasspathFiles = getInitialClasspath().getFiles();
        var sourceSetOutputFiles = getSourceSetOutput().getFiles();
        boolean foundSourceSetOutput = false;
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

                logger.debug("Modified classpath ({}):", getClass().getSimpleName());
                modifiedClasspath.getFiles().forEach(file -> logger.debug("  {}", file));
            }

            getSetter().get().setClasspath(task, modifiedClasspath);
        }

        return true;
    }

    @Override
    public String getDisplayName() {
        return TaskClasspathUpdater.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
