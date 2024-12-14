package name.remal.gradle_plugins.classes_relocation.intern.task;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskTransformer;
import name.remal.gradle_plugins.toolkit.LateInit;

public class TasksExecutor implements AutoCloseable {

    private final List<ImmediateTaskHandler<?, ?>> immediateTaskHandlers = new ArrayList<>();

    private final Set<QueuedTask> processedQueuedTasks = new LinkedHashSet<>();

    private final List<QueuedTaskTransformer> queuedTaskTransformers = new ArrayList<>();

    private final Queue<QueuedTask> queuedTasks = new PriorityQueue<>();

    private final List<QueuedTaskHandler<?>> queuedTaskHandlers = new ArrayList<>();

    private final LateInit<TaskExecutionContext> executionContext = lateInit("executionContext");


    public void addImmediateTaskHandlers(Iterable<? extends ImmediateTaskHandler<?, ?>> handlers) {
        StreamSupport.stream(handlers.spliterator(), false)
            .map(CachedImmediateTaskHandler::new)
            .forEach(immediateTaskHandlers::add);
        sort(immediateTaskHandlers);
    }

    public void addImmediateTaskHandlers(ImmediateTaskHandler<?, ?>... handlers) {
        addImmediateTaskHandlers(asList(handlers));
    }

    public void addImmediateTaskHandler(ImmediateTaskHandler<?, ?> handler) {
        addImmediateTaskHandlers(ImmutableList.of(handler));
    }

    public void addQueuedTaskTransformers(Iterable<? extends QueuedTaskTransformer> transformers) {
        StreamSupport.stream(transformers.spliterator(), false)
            .forEach(queuedTaskTransformers::add);
        sort(queuedTaskTransformers);
    }

    public void addQueuedTaskTransformers(QueuedTaskTransformer... transformers) {
        addQueuedTaskTransformers(asList(transformers));
    }

    public void addQueuedTaskTransformer(QueuedTaskTransformer transformer) {
        addQueuedTaskTransformers(ImmutableList.of(transformer));
    }

    public void addQueuedTaskHandlers(Iterable<? extends QueuedTaskHandler<?>> handlers) {
        StreamSupport.stream(handlers.spliterator(), false)
            .map(CachedQueuedTaskHandler::new)
            .forEach(queuedTaskHandlers::add);
        sort(queuedTaskHandlers);
    }

    public void addQueuedTaskHandlers(QueuedTaskHandler<?>... handlers) {
        addQueuedTaskHandlers(asList(handlers));
    }

    public void addQueuedTaskHandler(QueuedTaskHandler<?> handler) {
        addQueuedTaskHandlers(ImmutableList.of(handler));
    }

    public void setExecutionContext(TaskExecutionContext executionContext) {
        this.executionContext.set(executionContext);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
        for (ImmediateTaskHandler handler : immediateTaskHandlers) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, executionContext.get());
                if (result.isPresent()) {
                    return (RESULT) result.get();
                }
            }
        }

        throw new NotHandledTaskException(task);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void execute(QueuedTask task) {
        for (QueuedTaskHandler handler : queuedTaskHandlers) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, executionContext.get());
                if (result == TASK_HANDLED) {
                    return;
                }
            }
        }

        throw new NotHandledTaskException(task);
    }

    public void queue(QueuedTask task) {
        if (!processedQueuedTasks.add(task)) {
            return;
        }

        for (val transformer : queuedTaskTransformers) {
            val result = transformer.transform(task, executionContext.get());
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

            execute(task);
        }
    }

    @Override
    public void close() {
        for (val handler : queuedTaskHandlers) {
            handler.postProcess(executionContext.get());
        }
    }


    private static class CachedImmediateTaskHandler<RESULT, TASK extends ImmediateTask<RESULT>>
        implements ImmediateTaskHandler<RESULT, TASK> {

        @Delegate(excludes = BaseWithSupportedTaskClass.class)
        private final ImmediateTaskHandler<RESULT, TASK> delegate;

        @Getter
        private final Class<TASK> supportedTaskClass;

        public CachedImmediateTaskHandler(ImmediateTaskHandler<RESULT, TASK> delegate) {
            this.delegate = delegate;
            this.supportedTaskClass = delegate.getSupportedTaskClass();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

    }

    private static class CachedQueuedTaskHandler<TASK extends QueuedTask>
        implements QueuedTaskHandler<TASK> {

        @Delegate(excludes = BaseWithSupportedTaskClass.class)
        private final QueuedTaskHandler<TASK> delegate;

        @Getter
        private final Class<TASK> supportedTaskClass;

        public CachedQueuedTaskHandler(QueuedTaskHandler<TASK> delegate) {
            this.delegate = delegate;
            this.supportedTaskClass = delegate.getSupportedTaskClass();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

    }

}
