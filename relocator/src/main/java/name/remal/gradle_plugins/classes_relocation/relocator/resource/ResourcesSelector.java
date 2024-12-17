package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import org.jetbrains.annotations.Unmodifiable;

public interface ResourcesSelector extends ClassesRelocatorOrderedComponent {

    @Unmodifiable
    Optional<List<Resource>> select(
        String resourceName,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) throws Throwable;

}
