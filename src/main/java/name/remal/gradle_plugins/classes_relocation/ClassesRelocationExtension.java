package name.remal.gradle_plugins.classes_relocation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ClassesRelocationExtension implements ClassesRelocationSettings {

    {
        excludeClasses(
            "kotlin.Metadata"
        );
    }

}
