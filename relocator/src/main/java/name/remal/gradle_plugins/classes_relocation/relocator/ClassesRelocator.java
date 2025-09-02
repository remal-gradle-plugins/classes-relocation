package name.remal.gradle_plugins.classes_relocation.relocator;

import static com.google.common.collect.Lists.reverse;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.function.Predicate.not;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.ResourceContainer.newResourceContainerPaths;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.SystemClasspathUtils.getCurrentSystemClasspath;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.SystemClasspathUtils.getSystemClasspath;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.withMultiReleasePathPrefix;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInFunctionalTest;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorConfigurer;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorLifecycleComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ResourceContainer;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.WithSourceResources;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.ProcessSourceClass;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.license.CopyRelocationLicenses;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.manifest.CreateManifest;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.module_info.ProcessModuleInfo;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.CopySourceResource;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityUnmodifiableReport;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.TasksExecutor;
import name.remal.gradle_plugins.toolkit.CloseablesContainer;
import name.remal.gradle_plugins.toolkit.LateInit;
import org.gradle.api.logging.LogLevel;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

@SuperBuilder(toBuilder = true)
@CustomLog
public class ClassesRelocator extends ClassesRelocatorParams implements Closeable {

    private static final boolean IN_TEST = isInTest();

    private static final boolean IN_FUNCTIONAL_TEST = isInFunctionalTest();


    private final CloseablesContainer closeables = new CloseablesContainer();

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        closeables.close();
    }


    private final Classpath sourceClasspath = asLazyProxy(Classpath.class, () ->
        closeables.registerCloseable(newClasspathForPaths(singletonList(sourceJarPath)))
    );

    private final Classpath relocationClasspath = asLazyProxy(Classpath.class, () ->
        closeables.registerCloseable(newClasspathForPaths(relocationClasspathPaths))
    );

    private final Classpath sourceAndRelocationClasspath = asLazyProxy(Classpath.class, () ->
        sourceClasspath.plus(relocationClasspath)
    );

    private final Classpath compileAndRuntimeClasspath = asLazyProxy(Classpath.class, () ->
        closeables.registerCloseable(newClasspathForPaths(compileAndRuntimeClasspathPaths))
    );

    private final Classpath systemClasspath = asLazyProxy(Classpath.class, () -> {
        final Classpath classpath;
        if (jvmInstallationDir != null) {
            classpath = getSystemClasspath(jvmInstallationDir);
        } else {
            classpath = getCurrentSystemClasspath();
        }
        return closeables.registerCloseable(classpath);
    });

    private final ResourceContainer reachabilityMetadataResourceContainer = asLazyProxy(ResourceContainer.class, () ->
        closeables.registerCloseable(newResourceContainerPaths(reachabilityMetadataClasspathPaths))
    );

    private final RelocationOutput output = asLazyProxy(RelocationOutput.class, () ->
        closeables.registerCloseable(new RelocationOutputImpl(targetJarPath, metadataCharset, preserveFileTimestamps))
    );

    private final Set<Resource> processedResources = new LinkedHashSet<>();


    public void relocate() {
        checkThatNoSourceResourcesAreInRelocationPackages();

        var start = nanoTime();
        try {
            relocateImpl();

        } finally {
            var millis = NANOSECONDS.toMillis(nanoTime() - start);
            logger.log(
                IN_FUNCTIONAL_TEST ? LogLevel.QUIET : LogLevel.INFO,
                "Relocation took {}ms",
                millis
            );
        }
    }

    @SneakyThrows
    private void relocateImpl() {
        var context = new RelocationContextImpl();
        try (var implementations = new Implementations(context, objectFactory)) {
            context.setImplementations(implementations);

            reachabilityReport.set(context.getRelocationComponent(ReachabilityReport.class));

            var tasksExecutor = new TasksExecutor(context);
            context.setTasksExecutor(tasksExecutor);

            var configurers = context.getRelocationComponents(ClassesRelocatorConfigurer.class);
            for (var configurer : configurers) {
                configurer.configure(context);
            }

            var lifecycleComponents = context.getRelocationComponents(ClassesRelocatorLifecycleComponent.class);
            for (var lifecycleComponent : lifecycleComponents) {
                lifecycleComponent.prepareRelocation(context);
            }

            sourceClasspath.getClassNames().stream()
                .map(ProcessSourceClass::new)
                .forEach(tasksExecutor::queue);

            tasksExecutor.queue(new CopyRelocationLicenses());
            tasksExecutor.queue(new CreateManifest());
            tasksExecutor.queue(new ProcessModuleInfo());

            sourceClasspath.getAllResources().stream()
                .filter(not(context::isResourceProcessed))
                .map(Resource::getName)
                .filter(name -> !name.endsWith(".class")
                    && !name.equals(MANIFEST_NAME)
                    && !name.equals("module-info.class")
                    && !name.equals("META-INF/INDEX.LIST")
                )
                .map(CopySourceResource::new)
                .forEach(tasksExecutor::queue);

            tasksExecutor.executeQueuedTasks();

            for (var lifecycleComponent : reverse(lifecycleComponents)) {
                lifecycleComponent.finalizeRelocation(context);
            }
        }
    }

    private void checkThatNoSourceResourcesAreInRelocationPackages() {
        var sourcePackageNamesToRelocate = new LinkedHashSet<>(sourceClasspath.getPackageNames());
        var relocationPackageNames = relocationClasspath.getPackageNames();
        sourcePackageNamesToRelocate.retainAll(relocationPackageNames);
        if (!sourcePackageNamesToRelocate.isEmpty()) {
            throw new SourceResourcesInRelocationPackagesException(sourcePackageNamesToRelocate);
        }
    }


    private final LateInit<ReachabilityReport> reachabilityReport = lateInit("reachabilityReport");

    public ReachabilityUnmodifiableReport getReachabilityReport() {
        return new ReachabilityUnmodifiableReport(reachabilityReport.get());
    }


    private class RelocationContextImpl implements RelocationContext {

        private final LateInit<Implementations> implementations = lateInit("implementations");

        void setImplementations(Implementations implementations) {
            this.implementations.set(implementations);
        }


        private final LateInit<TasksExecutor> tasksExecutor = lateInit("tasksExecutor");

        void setTasksExecutor(TasksExecutor tasksExecutor) {
            this.tasksExecutor.set(tasksExecutor);
        }


        @Override
        public String getBasePackageForRelocatedClasses() {
            return basePackageForRelocatedClasses;
        }

        @Getter
        private final String relocatedClassNamePrefix =
            RelocationContext.super.getRelocatedClassNamePrefix();

        @Getter
        private final String relocatedClassInternalNamePrefix =
            RelocationContext.super.getRelocatedClassInternalNamePrefix();

        @Getter
        private final String relocatedResourceNamePrefix =
            RelocationContext.super.getRelocatedResourceNamePrefix();

        @Override
        public Classpath getSourceClasspath() {
            return sourceClasspath;
        }

        @Override
        public Classpath getRelocationClasspath() {
            return relocationClasspath;
        }

        @Override
        public Classpath getSourceAndRelocationClasspath() {
            return sourceAndRelocationClasspath;
        }

        @Override
        public Classpath getCompileAndRuntimeClasspath() {
            return compileAndRuntimeClasspath;
        }

        @Override
        public Classpath getSystemClasspath() {
            return systemClasspath;
        }

        @Override
        public ResourceContainer getReachabilityMetadataResourceContainer() {
            return reachabilityMetadataResourceContainer;
        }

        @Nullable
        @Override
        public String getModuleIdentifier(Resource resource) {
            return Optional.ofNullable(resource.getClasspathElement())
                .map(ClasspathElement::getPath)
                .map(Path::toUri)
                .map(moduleIdentifiers::get)
                .orElse(null);
        }


        @Override
        @UnmodifiableView
        public Set<Resource> getProcessedResources() {
            return unmodifiableSet(processedResources);
        }

        @Override
        public boolean markResourceAsProcessed(Resource resource) {
            if (!processedResources.add(resource)) {
                return false;
            }

            if (resource instanceof WithSourceResources) {
                boolean result = false;
                var sourceResources = ((WithSourceResources) resource).getSourceResources();
                for (var sourceResource : sourceResources) {
                    if (markResourceAsProcessed(sourceResource)) {
                        result = true;
                    }
                }
                return result;
            }

            return true;
        }

        @Override
        @SneakyThrows
        @SuppressWarnings("Slf4jFormatShouldBeConst")
        public void writeToOutput(Resource resource) {
            markResourceAsProcessed(resource);

            var originalResource = getOriginalResource(resource);
            if (originalResource != resource) {
                registerOriginalResource(resource, originalResource);
            }

            var resourceName = resource.getName();
            var fullResourceName = withMultiReleasePathPrefix(resourceName, resource.getMultiReleaseVersion());
            if (output.isResourceAdded(fullResourceName)) {
                var message = format(
                    "A resource was already relocated, ignoring duplicated path `%s` (source resource: %s)",
                    fullResourceName,
                    resource
                );
                if (IN_TEST) {
                    throw new IllegalStateException(message);
                } else if (getStringProperty("project.version").endsWith("-SNAPSHOT")) {
                    logger.warn(message, new IllegalStateException());
                } else {
                    logger.warn(message);
                }
                return;
            }

            var lastModifiedMillis = resource.getLastModifiedMillis();
            try (var in = resource.open()) {
                output.copy(fullResourceName, lastModifiedMillis, in);
            }
        }

        private Resource getOriginalResource(Resource resource) {
            while (resource instanceof WithSourceResources) {
                var sourceResources = ((WithSourceResources) resource).getSourceResources();
                if (sourceResources.size() == 1) {
                    resource = sourceResources.get(0);
                    continue;
                }
                break;
            }

            return resource;
        }


        @Override
        public <RESULT> Optional<RESULT> executeOptional(ImmediateTask<RESULT> task) {
            return tasksExecutor.get().executeOptional(task);
        }

        @Override
        public boolean markTaskAsProcessed(QueuedTask task) {
            return tasksExecutor.get().markAsProcessed(task);
        }

        @Override
        public boolean hasTaskQueued(Predicate<? super QueuedTask> predicate) {
            return tasksExecutor.get().hasTaskQueued(predicate);
        }

        @Override
        public void queue(QueuedTask task) {
            tasksExecutor.get().queue(task);
        }

        @Override
        public ClassesRelocatorConfig getConfig() {
            return config;
        }

        @Override
        @Unmodifiable
        public <T extends ClassesRelocatorComponent> List<T> getRelocationComponents(Class<T> type) {
            return implementations.get().getImplementations(type);
        }

    }

}
