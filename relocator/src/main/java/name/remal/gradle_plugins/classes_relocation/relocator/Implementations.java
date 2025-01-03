package name.remal.gradle_plugins.classes_relocation.relocator;

import static java.lang.String.format;
import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.ProcessSourceClassHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.license.CopyRelocationLicensesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.manifest.CreateManifestHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_classpath_element.MetaInfClasspathElementResourcesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services.MetaInfServicesResourcesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.module_info.ProcessModuleInfoHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.CopySourceResourceHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResourceHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassDescriptorHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassInternalNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ClassNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ResourceNameHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant.ProcessTypeConstantHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.xml.SimpleXmlResourcesHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.BaseWithSupportedTaskClass;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.toolkit.AbstractClosablesContainer;
import org.jetbrains.annotations.Unmodifiable;

class Implementations extends AbstractClosablesContainer {

    private final ClassesRelocatorObjectFactory objectFactory;
    private final Set<Class<? extends ClassesRelocatorComponent>> componentClasses;

    public Implementations(ClassesRelocatorObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
        this.componentClasses = ImmutableSet.of(
            RelocateClassHandler.class,
            CopySourceResourceHandler.class,
            CreateManifestHandler.class,
            ProcessModuleInfoHandler.class,
            ProcessSourceClassHandler.class,
            CopyRelocationLicensesHandler.class,
            MetaInfServicesResourcesHandler.class,
            MetaInfClasspathElementResourcesHandler.class,
            SimpleXmlResourcesHandler.class,
            RelocateResourceHandler.class,
            ClassInternalNameHandler.class,
            ClassDescriptorHandler.class,
            ClassNameHandler.class,
            ResourceNameHandler.class,
            ProcessTypeConstantHandler.class
        );
    }


    private final ConcurrentMap<Class<?>, Object> components = new ConcurrentHashMap<>();

    private Object getComponentFor(Class<?> type) {
        return components.computeIfAbsent(type, clazz -> {
            try {
                Object component = objectFactory.create(clazz);
                component = registerCloseableIfPossible(component);
                component = wrapWithCacheIfPossible(component);
                return component;
            } catch (Throwable e) {
                throw new ClassesRelocatorObjectInstantiationException(
                    format(
                        "Component of %s can't be instantiated",
                        clazz
                    ),
                    e
                );
            }
        });
    }


    private final ConcurrentMap<Class<?>, List<Object>> implementationsCache = new ConcurrentHashMap<>();

    @Unmodifiable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> List<T> getImplementations(Class<? extends ClassesRelocatorComponent> type) {
        return (List<T>) implementationsCache.computeIfAbsent(type, clazz -> {
            val impls = new ArrayList(componentClasses.size());
            for (val componentClass : componentClasses) {
                if (clazz.isAssignableFrom(componentClass)) {
                    val impl = clazz.cast(getComponentFor(componentClass));
                    impls.add(impl);
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


    private static Object wrapWithCacheIfPossible(Object component) {
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
