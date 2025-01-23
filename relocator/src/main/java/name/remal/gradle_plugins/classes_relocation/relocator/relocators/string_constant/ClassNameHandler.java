package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.StringHandlerUtils.isClassName;

import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateMethod;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ClassNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        var className = task.getString();
        if (!isClassName(className)
            || !context.isRelocationClassName(className)
        ) {
            return Optional.empty();
        }

        var classInternalName = toClassInternalName(className);
        var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
            .getClassInfo(classInternalName, context);
        classInfo.getConstructors().forEach(methodKey ->
            context.queue(new RelocateMethod(classInternalName, methodKey))
        );

        return Optional.of(context.getRelocatedClassNamePrefix() + className);
    }

}
