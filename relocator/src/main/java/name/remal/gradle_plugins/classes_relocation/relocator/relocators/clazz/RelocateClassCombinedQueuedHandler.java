package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

public class RelocateClassCombinedQueuedHandler
    implements QueuedTaskHandler<RelocateClassCombined> {

    @Override
    public QueuedTaskHandlerResult handle(RelocateClassCombined task, RelocationContext context) throws Throwable {
        context.execute(task);
        return TASK_HANDLED;
    }

}
