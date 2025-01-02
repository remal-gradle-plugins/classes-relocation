package name.remal.gradle_plugins.classes_relocation.relocator.context;

import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.TaskTransformContext;
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

    default boolean isRelocationClassName(String string) {
        return getRelocationClasspath().getClassNames().contains(string);
    }

    default boolean isRelocationClassInternalName(String string) {
        return getRelocationClasspath().getClassInternalNames().contains(string);
    }

    default boolean isRelocationResourceName(String string) {
        return getRelocationClasspath().getResources().containsKey(string);
    }

    @Nullable
    String getModuleIdentifier(Resource resource);

    @Nullable
    default String getRelocationSource(Resource resource) {
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


    void writeToOutput(Resource resource, @Nullable String updatedResourceName, @Nullable byte[] updatedContent);

    default void writeToOutput(Resource resource, String updatedResourceName) {
        writeToOutput(resource, updatedResourceName, null);
    }

    default void writeToOutput(Resource resource, byte[] updatedContent) {
        writeToOutput(resource, null, updatedContent);
    }

    default void writeToOutput(Resource resource) {
        writeToOutput(resource, null, null);
    }


    String getOriginalResourceName(String resourceName);

    void registerOriginalResourceName(String resourceName, String originalResourceName);

    default void registerOriginalResourceName(Resource resource, String originalResourceName) {
        registerOriginalResourceName(resource.getName(), originalResourceName);
    }


    <RESULT> Optional<RESULT> executeOptional(ImmediateTask<RESULT> task);

    <RESULT> RESULT execute(ImmediateTask<RESULT> task);

    default <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultValue) {
        return executeOptional(task).orElse(defaultValue);
    }

    void queue(QueuedTask task);


    @Unmodifiable
    <T extends ClassesRelocatorComponent> List<T> getRelocationComponents(Class<T> type);

}
