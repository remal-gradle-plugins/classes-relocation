package name.remal.gradle_plugins.classes_relocation;

import javax.inject.Inject;
import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Nested;

@Getter
public abstract class ClassesRelocationExtension implements ClassesRelocationSettings {

    {
        excludeClasses(
            "kotlin.Metadata"
        );
    }


    @Nested
    private final ClassesRelocationSourceSetClasspaths sourceSetClasspaths =
        getObjects().newInstance(ClassesRelocationSourceSetClasspaths.class);

    public void sourceSetClasspaths(Action<? super ClassesRelocationSourceSetClasspaths> action) {
        action.execute(sourceSetClasspaths);
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
