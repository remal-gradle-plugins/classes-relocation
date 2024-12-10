package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

@Getter
@Setter
public abstract class ClassRelocationForkOptions {

    static final boolean IS_FORK_ENABLED_DEFAULT = !isInTest();


    @Internal
    public abstract Property<Boolean> getEnabled();

    {
        getEnabled().convention(IS_FORK_ENABLED_DEFAULT);
    }


    @Internal
    public abstract Property<String> getMaxHeapSize();

}
