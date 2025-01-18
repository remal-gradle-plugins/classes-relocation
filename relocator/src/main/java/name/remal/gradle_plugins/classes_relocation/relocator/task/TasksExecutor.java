package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static java.lang.String.format;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;

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
                var result = handler.handle(task, context);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }


    public boolean markAsProcessed(QueuedTask task) {
        if (task instanceof AbstractQueuedIdentityTask) {
            throw new IllegalArgumentException(format(
                "Tasks of %s can't be marked as processed, as their processing state is not stored",
                AbstractQueuedIdentityTask.class
            ));
        }

        return processedQueuedTasks.add(task);
    }

    public boolean hasTaskQueued(Predicate<? super QueuedTask> predicate) {
        return queuedTasks.stream().anyMatch(predicate);
    }

    @SneakyThrows
    public void queue(QueuedTask task) {
        if (!(task instanceof AbstractQueuedIdentityTask)
            && !processedQueuedTasks.add(task)
        ) {
            return;
        }

        for (var transformer : context.getRelocationComponents(QueuedTaskTransformer.class)) {
            var transformed = transformer.transform(task, context);
            if (transformed.isPresent()) {
                for (var newTask : transformed.get()) {
                    queue(newTask);
                }
                return;
            }
        }

        queuedTasks.add(task);
    }


    public void executeQueuedTasks() {
        while (true) {
            var task = queuedTasks.poll();
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
                var result = handler.handle(task, context);
                if (result == TASK_HANDLED) {
                    task.onHandled();
                    return;
                }
            }
        }

        task.onNotHandled();
    }

}
