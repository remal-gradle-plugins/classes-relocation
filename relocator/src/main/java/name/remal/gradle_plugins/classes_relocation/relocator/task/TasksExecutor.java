package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;

@RequiredArgsConstructor
public class TasksExecutor {

    private final RelocationContext context;


    private final Set<QueuedTask> processedQueuedTasks = new LinkedHashSet<>();

    private final Queue<QueuedTask> queuedTasks = new PriorityQueue<>();


    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <RESULT> Optional<RESULT> executeOptional(ImmediateTask<RESULT> task) {
        for (ImmediateTaskHandler handler : context.getRelocationComponents(ImmediateTaskHandler.class)) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, context);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }

    public <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
        val result = executeOptional(task);
        if (result.isPresent()) {
            return result.get();
        }

        throw new NotHandledTaskException(task);
    }


    @SneakyThrows
    public void queue(QueuedTask task) {
        if (!processedQueuedTasks.add(task)) {
            return;
        }

        queuedTasks.add(task);
    }


    public void executeQueuedTasks() {
        while (true) {
            val task = queuedTasks.poll();
            if (task == null) {
                break;
            }

            executeQueuedTask(task);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeQueuedTask(QueuedTask task) {
        for (QueuedTaskHandler handler : context.getRelocationComponents(QueuedTaskHandler.class)) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, context);
                if (result == TASK_HANDLED) {
                    return;
                }
            }
        }

        throw new NotHandledTaskException(task);
    }

}
