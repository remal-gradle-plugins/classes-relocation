package name.remal.gradle_plugins.classes_relocation.relocator.task;

import java.util.List;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;

public interface QueuedTaskTransformer extends ClassesRelocatorOrderedComponent {

    Optional<List<QueuedTask>> transform(QueuedTask action, TaskTransformContext context) throws Throwable;

}
