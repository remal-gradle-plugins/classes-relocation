package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.StringHandlerUtils.isClassInternalName;

import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateMethod;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ClassDescriptorHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        var string = task.getString();
        if (string.length() < 3
            || string.charAt(string.length() - 1) != ';'
        ) {
            return Optional.empty();
        }

        int startPos = 0;
        while (string.charAt(startPos) == '[') {
            startPos++;
        }
        if (string.charAt(startPos) != 'L') {
            return Optional.empty();
        }
        startPos++;

        var endPos = string.length() - 1;
        if (startPos >= endPos) {
            return Optional.empty();
        }

        var classInternalName = string.substring(startPos, endPos);
        if (!isClassInternalName(classInternalName)
            || !context.isRelocationClassInternalName(classInternalName)
        ) {
            return Optional.empty();
        }

        var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
            .getClassInfo(classInternalName, context);
        classInfo.getConstructors().forEach(methodKey ->
            context.queue(new RelocateMethod(classInternalName, methodKey))
        );

        return Optional.of(
            string.substring(0, startPos)
                + context.getRelocatedClassInternalNamePrefix()
                + classInternalName
                + ';'
        );
    }

}
