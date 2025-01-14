package name.remal.gradle_plugins.classes_relocation.relocator.api;

public interface ClassesRelocatorLifecycleComponent
    extends ClassesRelocatorOrderedComponent {

    default void prepareRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

    default void finalizeRelocation(RelocationContext context) throws Throwable {
        // do nothing by default
    }

}
