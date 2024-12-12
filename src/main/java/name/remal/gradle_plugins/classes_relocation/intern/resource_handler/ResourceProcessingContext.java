package name.remal.gradle_plugins.classes_relocation.intern.resource_handler;

public interface ResourceProcessingContext {

    String getRelocatedClassNamePrefix();

    String getRelocatedClassInternalNamePrefix();

    void handleResourceName(String resourceName, String relocatedResourceName);

    void handleInternalClassName(String internalClassName);

}
