package name.remal.gradle_plugins.classes_relocation;

import static java.lang.Boolean.FALSE;
import static name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils.getJvmLanguagesCompileTaskProperties;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;

@RequiredArgsConstructor
abstract class RelocatorAnnotationsFileRegisterer implements Spec<Task>, Describable {

    public abstract Property<Boolean> getEnabled();

    public abstract RegularFileProperty getRelocatorAnnotationsFile();

    private void adjustClasspath(Task task) {
        if (FALSE.equals(getEnabled().getOrNull())) {
            return;
        }

        var compilationProperties = getJvmLanguagesCompileTaskProperties(task);
        if (compilationProperties != null) {
            var classpath = compilationProperties.getClasspath();
            classpath = classpath.plus(getObjects().fileCollection().from(getRelocatorAnnotationsFile()));
            compilationProperties.setClasspath(classpath);
        }
    }

    public boolean isSatisfiedBy(Task task) {
        adjustClasspath(task);
        return true;
    }


    @Override
    public String getDisplayName() {
        return RelocatorAnnotationsFileRegisterer.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
