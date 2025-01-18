package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ResourceContainer;
import name.remal.gradle_plugins.classes_relocation.relocator.metadata.OriginalResourceNames;
import name.remal.gradle_plugins.classes_relocation.relocator.metadata.OriginalResourceSources;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.NotHandledTaskException;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.TaskTransformContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface RelocationContext extends TaskTransformContext {

    String getBasePackageForRelocatedClasses();

    default String getRelocatedClassNamePrefix() {
        return getBasePackageForRelocatedClasses() + ".";
    }

    default String getRelocatedClassInternalNamePrefix() {
        return toClassInternalName(getRelocatedClassNamePrefix());
    }

    default String getRelocatedResourceNamePrefix() {
        return getRelocatedClassInternalNamePrefix();
    }


    Classpath getSourceClasspath();

    Classpath getRelocationClasspath();

    Classpath getSourceAndRelocationClasspath();

    Classpath getCompileAndRuntimeClasspath();

    Classpath getSystemClasspath();

    default boolean isRelocationClassName(@Nullable String string) {
        return string != null && getRelocationClasspath().getClassNames().contains(string);
    }

    default boolean isRelocationClassInternalName(@Nullable String string) {
        return string != null && getRelocationClasspath().getClassInternalNames().contains(string);
    }

    default boolean isRelocationResourceName(@Nullable String string) {
        return string != null && getRelocationClasspath().getResources().containsKey(string);
    }

    ResourceContainer getReachabilityMetadataResourceContainer();

    @Nullable
    String getModuleIdentifier(Resource resource);

    @Nullable
    default String getRelocationSource(Resource resource) {
        val source = getOriginalResourceSource(resource);
        if (source != null) {
            return source;
        }

        val classpathElement = resource.getClasspathElement();
        if (classpathElement == null) {
            return null;
        }

        val moduleIdentifier = getModuleIdentifier(resource);
        if (isNotEmpty(moduleIdentifier)) {
            return moduleIdentifier;
        }

        return classpathElement.getModuleName();
    }


    @UnmodifiableView
    Set<Resource> getProcessedResources();

    default boolean isResourceProcessed(Resource resource) {
        return getProcessedResources().contains(resource);
    }

    boolean markResourceAsProcessed(Resource resource);

    @Contract("_->param1")
    default Resource withResourceMarkedAsProcessed(Resource resource) {
        markResourceAsProcessed(resource);
        return resource;
    }


    void writeToOutput(Resource resource);


    @Nullable
    default String getOriginalResourceSource(String resourceName) {
        return getRelocationComponent(OriginalResourceSources.class)
            .getCurrent()
            .get(resourceName);
    }

    @Nullable
    default String getOriginalResourceSource(Resource resource) {
        return getOriginalResourceSource(resource.getName());
    }

    default void registerOriginalResourceSource(Resource resource, String relocationSource) {
        val currentSource = getOriginalResourceSource(resource);
        if (currentSource != null) {
            relocationSource = currentSource;
        }
        getRelocationComponent(OriginalResourceSources.class)
            .getNext()
            .put(resource.getName(), relocationSource);
    }

    default String getOriginalResourceName(String resourceName) {
        return getRelocationComponent(OriginalResourceNames.class)
            .getCurrent()
            .getOrDefault(resourceName, resourceName);
    }

    default String getOriginalResourceName(Resource resource) {
        return getOriginalResourceName(resource.getName());
    }

    default void registerOriginalResourceName(Resource resource, String originalResourceName) {
        val resourceName = resource.getName();
        originalResourceName = getOriginalResourceName(originalResourceName);
        if (!resourceName.equals(originalResourceName)) {
            getRelocationComponent(OriginalResourceNames.class)
                .getNext()
                .put(resourceName, originalResourceName);
        }
    }

    default void registerOriginalResource(Resource resource, Resource originalResource) {
        if (resource.getName().equals(originalResource.getName())) {
            return;
        }

        val relocationSource = getRelocationSource(originalResource);
        if (relocationSource != null) {
            registerOriginalResourceSource(resource, relocationSource);
        }

        registerOriginalResourceName(resource, originalResource.getName());
    }


    <RESULT> Optional<RESULT> executeOptional(ImmediateTask<RESULT> task);

    default <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultValue) {
        return executeOptional(task).orElse(defaultValue);
    }

    default <RESULT> RESULT execute(ImmediateTask<RESULT> task) {
        return executeOptional(task).orElseThrow(() -> new NotHandledTaskException(task));
    }


    boolean markTaskAsProcessed(QueuedTask task);

    boolean hasTaskQueued(Predicate<? super QueuedTask> predicate);

    default <T extends QueuedTask> boolean hasTaskQueued(Class<T> taskType, Predicate<? super T> predicate) {
        return hasTaskQueued(task ->
            taskType.isInstance(task) && predicate.test(taskType.cast(task))
        );
    }

    void queue(QueuedTask task);


    ClassesRelocatorConfig getConfig();


    @Unmodifiable
    <T extends ClassesRelocatorComponent> List<T> getRelocationComponents(Class<T> type);

    default <T extends ClassesRelocatorComponent> T getRelocationComponent(Class<T> type) {
        val impls = getRelocationComponents(type);
        if (impls.isEmpty()) {
            throw new IllegalStateException("No component of " + type);
        } else if (impls.size() == 1) {
            return impls.get(0);
        } else {
            throw new IllegalStateException("Multiple components of " + type);
        }
    }

}
