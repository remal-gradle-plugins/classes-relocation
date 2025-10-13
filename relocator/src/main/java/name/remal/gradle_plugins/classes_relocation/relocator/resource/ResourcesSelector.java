package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import java.util.List;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import org.jspecify.annotations.Nullable;

public interface ResourcesSelector extends ClassesRelocatorOrderedComponent {

    Optional<Resource> select(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        List<Resource> candidateResources,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) throws Throwable;

}
