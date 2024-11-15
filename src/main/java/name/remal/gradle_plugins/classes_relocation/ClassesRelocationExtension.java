package name.remal.gradle_plugins.classes_relocation;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.provider.SetProperty;

@Getter
@Setter
public abstract class ClassesRelocationExtension {

    public abstract SetProperty<String> getExcludeClassNames();

    {
        getExcludeClassNames().add("kotlin.Metadata");
    }


    public abstract SetProperty<String> getExcludeResourceNames();

}
