package name.remal.gradle_plugins.classes_relocation;

import lombok.Getter;
import org.gradle.api.provider.Property;

@Getter
public abstract class ClassesRelocationExtension implements ClassesRelocationSettings {

    public abstract Property<Boolean> getAddRelocatorAnnotationsToCompilationClasspath();

    {
        getAddRelocatorAnnotationsToCompilationClasspath().convention(true);
    }

}
