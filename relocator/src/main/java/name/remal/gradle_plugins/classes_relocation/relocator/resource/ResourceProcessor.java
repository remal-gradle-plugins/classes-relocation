package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;

public interface ResourceProcessor extends ClassesRelocatorOrderedComponent {

    Optional<byte[]> processResource(Resource resource, RelocationContext context) throws Throwable;

}
