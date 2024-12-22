package name.remal.gradle_plugins.classes_relocation;

import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.tasks.Nested;

@Getter
public abstract class ClassesRelocationExtension implements ClassesRelocationSettings {

    @Nested
    public abstract ClassesRelocationSourceSetClasspaths getSourceSetClasspaths();

    public void sourceSetClasspaths(Action<? super ClassesRelocationSourceSetClasspaths> action) {
        action.execute(getSourceSetClasspaths());
    }

}
