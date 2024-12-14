package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import static name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant.StringHandlerUtils.isClassInternalName;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz.RelocateClass;

public class ClassInternalNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        val classInternalName = task.getString();
        if (!isClassInternalName(classInternalName)
            || !context.isRelocationClassInternalName(classInternalName)
        ) {
            return Optional.empty();
        }

        context.queue(new RelocateClass(classInternalName));

        return Optional.of(context.getRelocatedClassInternalNamePrefix() + classInternalName);
    }

}
