package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateMethod.relocateNoArgConstructor;
import static name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.StringHandlerUtils.isClassInternalName;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ClassDescriptorHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        val string = task.getString();
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

        val endPos = string.length() - 1;
        if (startPos >= endPos) {
            return Optional.empty();
        }

        val classInternalName = string.substring(startPos, endPos);
        if (!isClassInternalName(classInternalName)
            || !context.isRelocationClassInternalName(classInternalName)
        ) {
            return Optional.empty();
        }

        context.queue(relocateNoArgConstructor(classInternalName));

        return Optional.of(
            string.substring(0, startPos)
                + context.getRelocatedClassInternalNamePrefix()
                + classInternalName
                + ';'
        );
    }

}
