package name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource;

import static java.util.function.Predicate.not;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

public class CopySourceResourceHandler implements QueuedTaskHandler<CopySourceResource> {

    @Override
    public QueuedTaskHandlerResult handle(CopySourceResource task, RelocationContext context) {
        // FIXME: we need to process `META-INF/services/*`, not just copy these resources

        context.getSourceClasspath().getResources(task.getResourceName()).stream()
            .filter(not(context::isResourceProcessed))
            .forEach(context::writeToOutput);

        return TASK_HANDLED;
    }

}
