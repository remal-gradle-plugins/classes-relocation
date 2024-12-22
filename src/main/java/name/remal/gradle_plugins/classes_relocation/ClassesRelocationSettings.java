package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

interface ClassesRelocationSettings {

    @Input
    Property<String> getBasePackageForRelocatedClasses();

    @Nested
    ClassRelocationForkOptions getForkOptions();

    default void forkOptions(Action<? super ClassRelocationForkOptions> action) {
        action.execute(getForkOptions());
    }

}
