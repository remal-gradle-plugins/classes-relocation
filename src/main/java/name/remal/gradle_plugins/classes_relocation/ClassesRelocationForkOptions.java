package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

@Getter
@Setter
public abstract class ClassesRelocationForkOptions {

    @Internal
    public abstract Property<Boolean> getEnabled();

    {
        boolean isEnabledByDefault = true;
        if (isInTest()) {
            isEnabledByDefault = false;
        }
        if (getStringProperty("project.version").endsWith("-SNAPSHOT")) {
            isEnabledByDefault = false;
        }
        getEnabled().convention(isEnabledByDefault);
    }


    @Internal
    public abstract Property<String> getMaxHeapSize();

}
