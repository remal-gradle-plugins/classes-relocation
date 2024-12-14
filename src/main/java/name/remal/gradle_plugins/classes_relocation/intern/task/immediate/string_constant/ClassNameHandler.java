package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import static name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.StringHandlerUtils.isClassName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.task.TaskExecutionContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz.RelocateClass;

public class ClassNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, TaskExecutionContext context) {
        val className = task.getString();
        if (!isClassName(className)
            || !context.isRelocationClassName(className)
        ) {
            return Optional.empty();
        }

        context.queue(new RelocateClass(toClassInternalName(className)));

        return Optional.of(context.getRelocatedClassNamePrefix() + className);
    }

}
