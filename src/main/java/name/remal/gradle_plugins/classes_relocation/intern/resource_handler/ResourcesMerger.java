package name.remal.gradle_plugins.classes_relocation.intern.resource_handler;

import java.util.Collection;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

public interface ResourcesMerger extends ResourceHandler {

    byte[] merge(String resourceName, Collection<? extends Resource> resources) throws Throwable;

}
