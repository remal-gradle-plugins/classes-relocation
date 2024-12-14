package name.remal.gradle_plugins.classes_relocation.intern.task;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.ClassDescriptorHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.ClassInternalNameHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.ClassNameHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.ResourceNameHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskTransformer;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz.ProcessSourceClassHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz.RelocateClassHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.meta_inf_services.RelocateMetaInfServicesHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource.CopySourceResourceHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource.RelocateResourceHandler;
import name.remal.gradle_plugins.toolkit.LateInit;

public class TasksExecutor implements Closeable {

    private final List<ImmediateTaskHandler<?, ?>> immediateTaskHandlers = new ArrayList<>();

    private final Set<QueuedTask> processedQueuedTasks = new LinkedHashSet<>();

    private final List<QueuedTaskTransformer> queuedTaskTransformers = new ArrayList<>();

    private final Queue<QueuedTask> queuedTasks = new PriorityQueue<>();

    private final List<QueuedTaskHandler<?>> queuedTaskHandlers = new ArrayList<>();

    private final LateInit<RelocationContext> executionContext = lateInit("executionContext");


    {
        addImmediateTaskHandlers(
            new ClassInternalNameHandler(),
            new ClassNameHandler(),
            new ClassDescriptorHandler(),
            new ResourceNameHandler()
        );

        addQueuedTaskHandlers(
            new ProcessSourceClassHandler(),
            new RelocateClassHandler(),
            new RelocateMetaInfServicesHandler(),
            new CopySourceResourceHandler(),
            new RelocateResourceHandler()
        );
    }


    public void addImmediateTaskHandlers(Iterable<? extends ImmediateTaskHandler<?, ?>> handlers) {
        StreamSupport.stream(handlers.spliterator(), false)
            .filter(Objects::nonNull)
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
            .filter(Objects::nonNull)
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
            .filter(Objects::nonNull)
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

    public void setExecutionContext(RelocationContext executionContext) {
        this.executionContext.set(executionContext);
    }


    public <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
        return executeImmediateTask(task, null);
    }

    public <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultResult) {
        return executeImmediateTask(task, defaultResult);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <RESULT> RESULT executeImmediateTask(
        ImmediateTask<RESULT> task,
        @Nullable RESULT defaultResult
    ) {
        for (ImmediateTaskHandler handler : immediateTaskHandlers) {
            if (handler.getSupportedTaskClass().isAssignableFrom(task.getClass())) {
                val result = handler.handle(task, executionContext.get());
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

            executeQueuedTask(task);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeQueuedTask(QueuedTask task) {
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
