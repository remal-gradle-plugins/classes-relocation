package name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource;

import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;

public class CopySourceResourceHandler implements QueuedTaskHandler<CopySourceResource> {

    @Override
    public QueuedTaskHandlerResult handle(CopySourceResource task, RelocationContext context) {
        context.getSourceClasspath().getResources(task.getResourceName()).stream()
            .filter(not(context::isResourceProcessed))
            .forEach(context::writeToOutput);

        return TASK_HANDLED;
    }

}
