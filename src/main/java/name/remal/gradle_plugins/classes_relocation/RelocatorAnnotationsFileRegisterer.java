package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils.getJvmLanguagesCompileTaskProperties;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.specs.Spec;

@RequiredArgsConstructor
abstract class RelocatorAnnotationsFileRegisterer implements Spec<Task>, Describable {

    public abstract RegularFileProperty getRelocatorAnnotationsFile();

    public boolean isSatisfiedBy(Task task) {
        var compilationProperties = getJvmLanguagesCompileTaskProperties(task);
        if (compilationProperties != null) {
            var classpath = compilationProperties.getClasspath();
            classpath = classpath.plus(getObjects().fileCollection().from(getRelocatorAnnotationsFile()));
            compilationProperties.setClasspath(classpath);
        }

        return true;
    }

    @Override
    public String getDisplayName() {
        return RelocatorAnnotationsFileRegisterer.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
