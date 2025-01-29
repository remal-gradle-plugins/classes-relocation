package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

interface ClassesRelocationSettings {

    @Input
    Property<String> getBasePackageForRelocatedClasses();

    @Console
    Property<Boolean> getLogDynamicReflectionUsage();

    @Nested
    ClassesRelocationSettingsMinimize getMinimize();

    default void minimize(Action<? super ClassesRelocationSettingsMinimize> action) {
        action.execute(getMinimize());
    }

    @Nested
    ClassesRelocationForkOptions getFork();

    default void fork(Action<? super ClassesRelocationForkOptions> action) {
        action.execute(getFork());
    }

}
