package name.remal.gradle_plugins.classes_relocation.relocator.api;

import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;

public interface ClassesRelocatorComponent {

    default void prepareRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

    default void finalizeRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

}
