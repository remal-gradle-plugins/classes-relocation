package name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassName;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;
import org.objectweb.asm.Type;

public class ProcessTypeConstantHandler implements ImmediateTaskHandler<Type, ProcessTypeConstant> {

    @Override
    public Optional<Type> handle(ProcessTypeConstant task, RelocationContext context) throws Throwable {
        val type = task.getType();
        if (type.getSort() == Type.OBJECT) {
            val internalName = type.getInternalName();
            if (context.isRelocationClassInternalName(internalName)
                && !internalName.equals("org/codehaus/groovy/runtime/ExtensionModule")
            ) {
                context.executeOptional(new RelocateResource(
                    "META-INF/services/" + toClassName(internalName),
                    task.getClassResource().getClasspathElement()
                ));
            }
        }

        return Optional.of(type);
    }

}
