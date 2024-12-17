package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import java.util.Collection;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;

public interface ResourcesMerger extends ClassesRelocatorOrderedComponent {

    Optional<byte[]> merge(String resourceName, Collection<? extends Resource> resources) throws Throwable;

}
