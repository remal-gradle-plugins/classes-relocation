package name.remal.gradle_plugins.classes_relocation;

import static lombok.AccessLevel.PUBLIC;

import java.io.File;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class TaskClasspathUpdater implements Spec<Task>, Describable {

    public abstract Property<TaskClasspathGetter<Task>> getGetter();

    public abstract ConfigurableFileCollection getSourceSetOutput();

    public abstract RegularFileProperty getJarFile();

    public abstract Property<TaskClasspathSetter<Task>> getSetter();

    @Override
    public boolean isSatisfiedBy(Task task) {
        execute(task);
        return true;
    }

    @SneakyThrows
    private void execute(Task task) {
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
        var modifiedClasspath = new LinkedHashSet<File>(initialClasspathFiles.size());
        for (var file : initialClasspathFiles) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    modifiedClasspath.add(getJarFile().get().getAsFile());
                    foundSourceSetOutput = true;
                }
            } else {
                modifiedClasspath.add(file);
            }
        }

        if (foundSourceSetOutput) {
            var logger = task.getLogger();
            if (logger.isDebugEnabled()) {
                logger.debug("Initial classpath:");
                initialClasspathFiles.forEach(file -> logger.debug("  {}", file));

                logger.debug("Modified classpath:");
                modifiedClasspath.forEach(file -> logger.debug("  {}", file));
            }

            var modifiedClasspathFileCollection = getObjects().fileCollection().from(modifiedClasspath);
            getSetter().get().setClasspath(task, modifiedClasspathFileCollection);
        }
    }


    @Override
    public String getDisplayName() {
        return TaskClasspathUpdater.class.getSimpleName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
