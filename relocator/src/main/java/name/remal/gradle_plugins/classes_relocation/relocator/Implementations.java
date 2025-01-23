package name.remal.gradle_plugins.classes_relocation.relocator;

import static java.lang.String.format;
import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Delegate;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.metadata.OriginalResourceNames;
import name.remal.gradle_plugins.classes_relocation.relocator.metadata.OriginalResourceSources;
import name.remal.gradle_plugins.classes_relocation.relocator.minimization.ClassReachabilityConfigs;
import name.remal.gradle_plugins.classes_relocation.relocator.minimization.MinimizationConfigConfigurer;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.ProcessSourceClassHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassCombinedImmediateHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassCombinedQueuedHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClassTaskTransformer;
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
            MinimizationConfigConfigurer.class,
            OriginalResourceNames.class,
            OriginalResourceSources.class,
            ClassReachabilityConfigs.class,
            ClassInfoComponent.class,
            RelocateClassTaskTransformer.class,
            RelocateClassCombinedImmediateHandler.class,
            RelocateClassCombinedQueuedHandler.class,
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
    @SuppressWarnings({"unchecked", "rawtypes", "java:S3776"})
    public <T> List<T> getImplementations(Class<? extends ClassesRelocatorComponent> type) {
        return (List<T>) implementationsCache.computeIfAbsent(type, clazz -> {
            var impls = new ArrayList(componentClasses.size());
            for (var componentClass : componentClasses) {
                if (clazz.isAssignableFrom(componentClass)) {
                    Object impl = getComponentFor(componentClass);
                    if (!clazz.isInstance(impl) && impl instanceof WithDelegate<?>) {
                        var delegate = ((WithDelegate<?>) impl).delegate();
                        if (clazz.isInstance(delegate)) {
                            impl = delegate;
                        }
                    }
                    impls.add(clazz.cast(impl));
                }
            }
            if (Comparable.class.isAssignableFrom(clazz)) {
                impls.sort(naturalOrder());
            }
            return List.copyOf(impls);
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

    private interface WithDelegate<T> {
        T delegate();
    }

    private static class CachedImmediateTaskHandler<RESULT, TASK extends ImmediateTask<RESULT>>
        implements ImmediateTaskHandler<RESULT, TASK>, WithDelegate<ImmediateTaskHandler<RESULT, TASK>> {

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

        @Override
        public ImmediateTaskHandler<RESULT, TASK> delegate() {
            return delegate;
        }

    }

    private static class CachedQueuedTaskHandler<TASK extends QueuedTask>
        implements QueuedTaskHandler<TASK>, WithDelegate<QueuedTaskHandler<TASK>> {

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

        @Override
        public QueuedTaskHandler<TASK> delegate() {
            return delegate;
        }

    }

}
