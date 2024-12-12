package name.remal.gradle_plugins.classes_relocation.intern.resource_handler;

import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

public interface ResourceProcessor extends ResourceHandler {

    byte[] processResource(Resource resource, ResourceProcessingContext context) throws Throwable;

}
