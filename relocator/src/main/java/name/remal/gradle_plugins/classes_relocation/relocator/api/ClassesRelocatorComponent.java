package name.remal.gradle_plugins.classes_relocation.relocator.api;

import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;

public interface ClassesRelocatorComponent {

    default void beforeRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

    default void afterRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

}
