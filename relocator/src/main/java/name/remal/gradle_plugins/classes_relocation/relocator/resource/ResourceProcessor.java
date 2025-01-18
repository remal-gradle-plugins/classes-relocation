package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import java.util.Optional;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;

public interface ResourceProcessor extends ClassesRelocatorOrderedComponent {

    Optional<Resource> processResource(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) throws Throwable;

}
