package name.remal.gradle_plugins.classes_relocation.intern.context;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import java.util.Set;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.task.TaskTransformContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;
import org.jetbrains.annotations.UnmodifiableView;

public interface RelocationContext extends TaskTransformContext {

    String getBasePackageForRelocatedClasses();

    default String getRelocatedClassNamePrefix() {
        return getBasePackageForRelocatedClasses() + ".";
    }

    default String getRelocatedClassInternalNamePrefix() {
        return toClassInternalName(getRelocatedClassNamePrefix());
    }


    Classpath getSourceClasspath();

    Classpath getRelocationClasspath();

    default Classpath getSourceAndRelocationClasspath() {
        return getSourceClasspath().plus(getRelocationClasspath());
    }

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


    <RESULT> RESULT execute(ImmediateTask<RESULT> task);

    <RESULT> RESULT execute(ImmediateTask<RESULT> task, RESULT defaultValue);

    void queue(QueuedTask task);

}
