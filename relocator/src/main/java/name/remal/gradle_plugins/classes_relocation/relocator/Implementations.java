package name.remal.gradle_plugins.classes_relocation.relocator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.ProcessSourceClassHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.license.CopyRelocationLicensesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.manifest.ProcessManifestHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services.MetaInfServicesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services.RelocateMetaInfServicesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.module_info.ProcessModuleInfoHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.CopySourceResourceHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResourceHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassDescriptorHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassInternalNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ResourceNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.BaseWithSupportedTaskClass;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.toolkit.AbstractClosablesContainer;
import org.jetbrains.annotations.Unmodifiable;

class Implementations extends AbstractClosablesContainer {

    private final List<? extends ClassesRelocatorComponent> components;

    public Implementations() {
        this.components = Stream.of(
                new RelocateClassHandler(),
                new RelocateResourceHandler(),
                new CopySourceResourceHandler(),
                new ProcessManifestHandler(),
                new ProcessModuleInfoHandler(),
                new ProcessSourceClassHandler(),
                new CopyRelocationLicensesHandler(),
                new RelocateMetaInfServicesHandler(),
                new MetaInfServicesHandler(),
                new ClassInternalNameHandler(),
                new ClassDescriptorHandler(),
                new ClassNameHandler(),
                new ResourceNameHandler()
            )
            .map(this::registerCloseableIfPossible)
            .map(Implementations::wrapWithCacheIfPossible)
            .collect(toImmutableList());
    }


    private final ConcurrentMap<Class<?>, List<Object>> cache = new ConcurrentHashMap<>();

    @Unmodifiable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> List<T> getImplementations(Class<? extends ClassesRelocatorComponent> type) {
        return (List<T>) cache.computeIfAbsent(type, clazz -> {
            val impls = new ArrayList(components.size());
            for (val component : components) {
                if (clazz.isInstance(component)) {
                    impls.add(component);
                }
            }
            if (Comparable.class.isAssignableFrom(clazz)) {
                impls.sort(naturalOrder());
            }
            return ImmutableList.copyOf(impls);
        });
    }


    private <T> T registerCloseableIfPossible(T component) {
        if (component instanceof AutoCloseable) {
            registerCloseable((AutoCloseable) component);
        }
        return component;
    }


    private static ClassesRelocatorComponent wrapWithCacheIfPossible(ClassesRelocatorComponent component) {
        if (component instanceof ImmediateTaskHandler<?, ?>) {
            return new CachedImmediateTaskHandler<>((ImmediateTaskHandler<?, ?>) component);

        } else if (component instanceof QueuedTaskHandler<?>) {
            return new CachedQueuedTaskHandler<>((QueuedTaskHandler<?>) component);

        } else {
            return component;
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
