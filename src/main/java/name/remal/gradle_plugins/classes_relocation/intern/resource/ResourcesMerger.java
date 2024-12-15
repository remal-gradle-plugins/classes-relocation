package name.remal.gradle_plugins.classes_relocation.intern.resource;

import java.util.Collection;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.intern.Ordered;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

public interface ResourcesMerger extends Ordered {

    Optional<byte[]> merge(String resourceName, Collection<? extends Resource> resources) throws Throwable;

}
