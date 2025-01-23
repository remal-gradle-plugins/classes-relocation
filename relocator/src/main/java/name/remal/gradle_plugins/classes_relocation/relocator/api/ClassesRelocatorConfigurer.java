package name.remal.gradle_plugins.classes_relocation.relocator.api;

public interface ClassesRelocatorConfigurer
    extends ClassesRelocatorOrderedComponent {

    default void configure(RelocationContext context) throws Throwable {
        // do nothing by default
    }

}
