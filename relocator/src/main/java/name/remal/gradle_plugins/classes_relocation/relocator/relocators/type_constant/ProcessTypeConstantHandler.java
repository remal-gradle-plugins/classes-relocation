package name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant;

import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services.RelocateMetaInfServices;
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
                context.queue(new RelocateMetaInfServices(internalName));
            }
        }

        return Optional.of(type);
    }

}
