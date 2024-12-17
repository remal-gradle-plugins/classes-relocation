package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.StringHandlerUtils.isClassName;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClass;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ClassNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
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
