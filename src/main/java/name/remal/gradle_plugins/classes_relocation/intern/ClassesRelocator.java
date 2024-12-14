package name.remal.gradle_plugins.classes_relocation.intern;

import static java.util.Collections.singletonList;
import static name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.withMultiReleasePathPrefix;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.task.TasksExecutor;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz.ProcessSourceClass;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;

@SuperBuilder
@CustomLog
public class ClassesRelocator extends ClassesRelocatorParams implements Closeable {

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
        {
            val sourcePackageNamesToRelocate = new LinkedHashSet<>(sourceClasspath.getPackageNames());
            val relocationPackageNames = relocationClasspath.getPackageNames();
            sourcePackageNamesToRelocate.retainAll(relocationPackageNames);
            if (!sourcePackageNamesToRelocate.isEmpty()) {
                throw new SourceResourcesInRelocationPackagesException(sourcePackageNamesToRelocate);
            }
        }

        try (val tasksExecutor = new TasksExecutor()) {
            val executionContext = new RelocationContextImpl(tasksExecutor);
            tasksExecutor.setExecutionContext(executionContext);

            sourceClasspath.getClassNames().stream()
                .map(ProcessSourceClass::new)
                .forEach(tasksExecutor::queue);

            tasksExecutor.executeQueuedTasks();
        }
    }


    @RequiredArgsConstructor
    private class RelocationContextImpl implements RelocationContext {

        private final TasksExecutor tasksExecutor;

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
        public boolean isResourceProcessed(Resource resource) {
            return processedResources.contains(resource);
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
            return tasksExecutor.execute(task);
        }

        @Override
        public <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultValue) {
            return tasksExecutor.execute(task, defaultValue);
        }

        @Override
        public void queue(QueuedTask task) {
            tasksExecutor.queue(task);
        }

    }

}
