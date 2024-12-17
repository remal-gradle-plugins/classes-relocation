package name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

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
