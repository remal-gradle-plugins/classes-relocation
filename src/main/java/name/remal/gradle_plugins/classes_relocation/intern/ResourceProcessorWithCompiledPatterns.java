package name.remal.gradle_plugins.classes_relocation.intern;

import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceProcessingContext;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceProcessor;

class ResourceProcessorWithCompiledPatterns extends ResourcesHandlerWithCompiledPatterns<ResourceProcessor> {

    public ResourceProcessorWithCompiledPatterns(ResourceProcessor processor) {
        super(processor);
    }

    @SneakyThrows
    public byte[] processResource(Resource resource, ResourceProcessingContext context) {
        return handler.processResource(resource, context);
    }

}
