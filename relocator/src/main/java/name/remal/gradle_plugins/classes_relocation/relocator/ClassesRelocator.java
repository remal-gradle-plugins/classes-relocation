package name.remal.gradle_plugins.classes_relocation.relocator;

import static java.lang.System.nanoTime;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.withMultiReleasePathPrefix;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInFunctionalTest;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.ProcessSourceClass;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.license.CopyRelocationLicenses;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.manifest.ProcessManifest;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.module_info.ProcessModuleInfo;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.CopySourceResource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.TasksExecutor;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;
import name.remal.gradle_plugins.toolkit.LateInit;
import org.gradle.api.logging.LogLevel;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

@SuperBuilder
@CustomLog
public class ClassesRelocator extends ClassesRelocatorParams implements Closeable {

    private static final boolean IS_IN_FUNCTIONAL_TEST = isInFunctionalTest();

    private final ClosablesContainer closables = new ClosablesContainer();

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        closables.close();
    }


    private final Classpath sourceClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(singletonList(sourceJarPath)))
    );

    private final Classpath relocationClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(relocationClasspathPaths))
    );

    private final RelocationOutput output = asLazyProxy(RelocationOutput.class, () ->
        closables.registerCloseable(new RelocationOutputImpl(targetJarPath, metadataCharset, preserveFileTimestamps))
    );

    private final Set<Resource> processedResources = new LinkedHashSet<>();


    public void relocate() {
        checkThatNoSourceResourcesAreInRelocationPackages();

        val start = nanoTime();
        try {
            relocateImpl();

        } finally {
            val millis = NANOSECONDS.toMillis(nanoTime() - start);
            logger.log(
                IS_IN_FUNCTIONAL_TEST ? LogLevel.QUIET : LogLevel.INFO,
                "Relocation took {}ms",
                millis
            );
        }
    }

    @SneakyThrows
    private void relocateImpl() {
        try (val implementations = new Implementations()) {
            val context = new RelocationContextImpl();
            context.setImplementations(implementations);

            val tasksExecutor = new TasksExecutor(context);
            context.setTasksExecutor(tasksExecutor);

            for (val handler : context.getRelocationComponents(QueuedTaskHandler.class)) {
                handler.beforeRelocation(context);
            }

            sourceClasspath.getClassNames().stream()
                .map(ProcessSourceClass::new)
                .forEach(tasksExecutor::queue);

            tasksExecutor.queue(new CopyRelocationLicenses());
            tasksExecutor.queue(new ProcessManifest());
            tasksExecutor.queue(new ProcessModuleInfo());

            sourceClasspath.getAllResources().stream()
                .map(Resource::getName)
                .filter(name -> !name.endsWith(".class")
                    && !name.equals(MANIFEST_NAME)
                    && !name.equals("module-info.class")
                    && !name.equals("META-INF/INDEX.LIST")
                )
                .map(CopySourceResource::new)
                .forEach(tasksExecutor::queue);

            tasksExecutor.executeQueuedTasks();

            for (val handler : context.getRelocationComponents(QueuedTaskHandler.class)) {
                handler.afterRelocation(context);
            }
        }
    }

    private void checkThatNoSourceResourcesAreInRelocationPackages() {
        val sourcePackageNamesToRelocate = new LinkedHashSet<>(sourceClasspath.getPackageNames());
        val relocationPackageNames = relocationClasspath.getPackageNames();
        sourcePackageNamesToRelocate.retainAll(relocationPackageNames);
        if (!sourcePackageNamesToRelocate.isEmpty()) {
            throw new SourceResourcesInRelocationPackagesException(sourcePackageNamesToRelocate);
        }
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

        @Override
        public Classpath getSourceClasspath() {
            return sourceClasspath;
        }

        @Override
        public Classpath getRelocationClasspath() {
            return relocationClasspath;
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

            if (resource instanceof GeneratedResource) {
                boolean result = false;
                val sourceResources = ((GeneratedResource) resource).getSourceResources();
                for (val sourceResource : sourceResources) {
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
        public void writeToOutput(
            Resource resource,
            @Nullable String updatedResourceName,
            @Nullable byte[] updatedContent
        ) {
            markResourceAsProcessed(resource);

            val resourceName = updatedResourceName != null ? updatedResourceName : resource.getName();
            val fullResourceName = withMultiReleasePathPrefix(resourceName, resource.getMultiReleaseVersion());
            if (output.isResourceAdded(fullResourceName)) {
                logger.warn(
                    "A resource was already relocated, ignoring duplicated path `{}` (source resource: {})",
                    fullResourceName,
                    resource
                );
                return;
            }

            val lastModifiedMillis = resource.getLastModifiedMillis();
            if (updatedContent != null) {
                output.write(fullResourceName, lastModifiedMillis, updatedContent);

            } else {
                try (val in = resource.open()) {
                    output.copy(fullResourceName, lastModifiedMillis, in);
                }
            }
        }

        @Override
        public <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
            return tasksExecutor.get().execute(task);
        }

        @Override
        public <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultValue) {
            return tasksExecutor.get().execute(task, defaultValue);
        }

        @Override
        public void queue(QueuedTask task) {
            tasksExecutor.get().queue(task);
        }


        @Override
        @Unmodifiable
        public <T extends ClassesRelocatorComponent> List<T> getRelocationComponents(Class<T> type) {
            return implementations.get().getImplementations(type);
        }

    }

}
