package name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassName;

import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateMethod;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;
import org.objectweb.asm.Type;

public class ProcessTypeConstantHandler implements ImmediateTaskHandler<Type, ProcessTypeConstant> {

    @Override
    public Optional<Type> handle(ProcessTypeConstant task, RelocationContext context) throws Throwable {
        var type = task.getType();
        if (type.getSort() == Type.OBJECT) {
            var classInternalName = type.getInternalName();
            if (context.isRelocationClassInternalName(classInternalName)) {
                var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
                    .getClassInfo(classInternalName, context);
                classInfo.getConstructors().forEach(methodKey ->
                    context.queue(new RelocateMethod(classInternalName, methodKey))
                );

                context.executeOptional(new RelocateResource(
                    "META-INF/services/" + toClassName(classInternalName),
                    task.getClassResource().getClasspathElement()
                ));
            }
        }

        return Optional.of(type);
    }

}
