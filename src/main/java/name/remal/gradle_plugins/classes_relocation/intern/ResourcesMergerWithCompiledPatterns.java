package name.remal.gradle_plugins.classes_relocation.intern;

import java.util.Collection;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourcesMerger;

class ResourcesMergerWithCompiledPatterns extends ResourcesHandlerWithCompiledPatterns<ResourcesMerger> {

    public ResourcesMergerWithCompiledPatterns(ResourcesMerger merger) {
        super(merger);
    }

    @SneakyThrows
    public byte[] merge(String resourceName, Collection<? extends Resource> resources) {
        return handler.merge(resourceName, resources);
    }

}
