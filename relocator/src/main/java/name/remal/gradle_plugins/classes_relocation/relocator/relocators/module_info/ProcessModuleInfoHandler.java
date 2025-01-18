package name.remal.gradle_plugins.classes_relocation.relocator.relocators.module_info;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

public class ProcessModuleInfoHandler implements QueuedTaskHandler<ProcessModuleInfo> {

    @Override
    public QueuedTaskHandlerResult handle(ProcessModuleInfo task, RelocationContext context) throws Throwable {
        val moduleInfoResources = context.getSourceClasspath().getResources("module-info.class");
        if (!moduleInfoResources.isEmpty()) {
            throw new IllegalStateException("JAR files with module-info can't be relocated yet");
        }

        return TASK_HANDLED;
    }

}
