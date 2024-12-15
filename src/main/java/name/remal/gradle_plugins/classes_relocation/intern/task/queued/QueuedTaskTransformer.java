package name.remal.gradle_plugins.classes_relocation.intern.task.queued;

import java.util.List;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.intern.Ordered;
import name.remal.gradle_plugins.classes_relocation.intern.task.TaskTransformContext;

public interface QueuedTaskTransformer extends Ordered {

    Optional<List<QueuedTask>> transform(QueuedTask action, TaskTransformContext context) throws Throwable;

}
