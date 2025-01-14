package name.remal.gradle_plugins.classes_relocation.relocator.task;

import java.util.Collection;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;

public interface QueuedTaskTransformer extends ClassesRelocatorOrderedComponent {

    Optional<Collection<? extends QueuedTask>> transform(QueuedTask task, RelocationContext context) throws Throwable;

}
