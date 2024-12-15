package name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource;

import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;

public class RelocateResourceHandler implements QueuedTaskHandler<RelocateResource> {

    @Override
    public QueuedTaskHandlerResult handle(RelocateResource task, RelocationContext context) {
        context.getRelocationClasspath().getResources(task.getResourceName()).stream()
            .filter(not(context::isResourceProcessed))
            .forEach(resource -> {
                context.writeToOutput(resource, task.getUpdatedResourceName());
            });

        return TASK_HANDLED;
    }

}
