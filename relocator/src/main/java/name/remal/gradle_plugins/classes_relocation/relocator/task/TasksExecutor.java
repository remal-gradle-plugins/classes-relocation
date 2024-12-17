package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;

@RequiredArgsConstructor
public class TasksExecutor {

    private final RelocationContext context;


    private final Set<QueuedTask> processedQueuedTasks = new LinkedHashSet<>();

    private final Queue<QueuedTask> queuedTasks = new PriorityQueue<>();


    public <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
        return executeImmediateTask(task, null);
    }

    public <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultResult) {
        return executeImmediateTask(task, defaultResult);
    }

    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <RESULT> RESULT executeImmediateTask(
        ImmediateTask<RESULT> task,
        @Nullable RESULT defaultResult
    ) {
        for (ImmediateTaskHandler handler : context.getRelocationComponents(ImmediateTaskHandler.class)) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, context);
                if (result.isPresent()) {
                    return (RESULT) result.get();
                }
            }
        }

        if (defaultResult != null) {
            return defaultResult;
        }

        throw new NotHandledTaskException(task);
    }


    @SneakyThrows
    public void queue(QueuedTask task) {
        if (!processedQueuedTasks.add(task)) {
            return;
        }

        for (val transformer : context.getRelocationComponents(QueuedTaskTransformer.class)) {
            val result = transformer.transform(task, context);
            if (result.isPresent()) {
                result.get().forEach(this::queue);
                return;
            }
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
