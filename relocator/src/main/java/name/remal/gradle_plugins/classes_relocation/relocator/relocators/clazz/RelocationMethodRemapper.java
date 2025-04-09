package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodType;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.CustomLog;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

@CustomLog
class RelocationMethodRemapper extends MethodRemapper {

    private static final Handle DEFAULT_LAMBDA_BOOSTRAP_METHOD_HANDLE = new Handle(
        H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory",
        "metafactory",
        "("
            + "Ljava/lang/invoke/MethodHandles$Lookup;"
            + "Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodHandle;"
            + "Ljava/lang/invoke/MethodType;"
            + ")"
            + "Ljava/lang/invoke/CallSite;",
        false
    );

    private static final Set<String> CLASS_DYNAMIC_REFLECTION_METHOD_NAMES = ImmutableSet.of(
        "forName",
        "getFields",
        "getConstructors",
        "getMethods",
        "getField",
        "getConstructor",
        "getMethod",
        "getDeclaredFields",
        "getDeclaredConstructors",
        "getDeclaredMethods",
        "getDeclaredField",
        "getDeclaredConstructor",
        "getDeclaredMethod"
    );


    @Nullable
    private final String classInternalName;

    private final RelocationContext context;

    public RelocationMethodRemapper(
        int api,
        @Nullable MethodVisitor methodVisitor,
        Remapper remapper,
        @Nullable String classInternalName,
        RelocationContext context
    ) {
        super(api, methodVisitor, remapper);
        if (classInternalName == null) {
            if (remapper instanceof RelocationRemapper) {
                classInternalName = ((RelocationRemapper) remapper).getClassInternalName();
            }
        }
        this.classInternalName = classInternalName;
        this.context = context;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (context.isRelocationClassInternalName(owner)) {
            context.queue(new RelocateField(owner, name));
        }

        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(
        int opcodeAndSource,
        String owner,
        String name,
        String descriptor,
        boolean isInterface
    ) {
        var methodKey = methodKeyOf(name, descriptor);
        if (context.isRelocationClassInternalName(owner)) {
            context.queue(new RelocateMethod(owner, name, descriptor));

        } else if (!methodKey.isConstructor()) {
            var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
                .getClassInfo(owner, context);
            classInfo.getAllParentClasses().stream()
                .filter(parentInfo -> context.isRelocationClassInternalName(parentInfo.getInternalClassName()))
                .forEach(parentInfo -> {
                    if (parentInfo.hasAccessibleMethod(methodKey)) {
                        context.queue(new RelocateMethod(parentInfo.getInternalClassName(), methodKey));
                    }
                });
        }

        if (context.getConfig().isLogDynamicReflectionUsage()
            && context.isRelocationClassInternalName(classInternalName)
        ) {
            if (owner.equals("java/lang/Class") && CLASS_DYNAMIC_REFLECTION_METHOD_NAMES.contains(name)) {
                logger.warn(
                    "Class {} calls a dynamic reflection method: {}.{}{}",
                    classInternalName,
                    owner,
                    name,
                    descriptor
                );
            }
        }

        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(
        String name,
        String descriptor,
        Handle bootstrapMethodHandle,
        Object... bootstrapMethodArguments
    ) {
        do {
            if (!descriptor.startsWith("()L")) {
                break;
            }

            var ownerInternalName = getMethodType(descriptor).getReturnType().getInternalName();
            if (!context.isRelocationClassInternalName(ownerInternalName)) {
                break;
            }

            var classInfo = context.getRelocationComponent(ClassInfoComponent.class)
                .getClassInfo(ownerInternalName, context);

            if (DEFAULT_LAMBDA_BOOSTRAP_METHOD_HANDLE.equals(bootstrapMethodHandle)
                && bootstrapMethodArguments.length == 3
                && bootstrapMethodArguments[0] instanceof Type
                && ((Type) bootstrapMethodArguments[0]).getSort() == Type.METHOD
            ) {
                var methodDescriptor = ((Type) bootstrapMethodArguments[0]).getDescriptor();
                var methodKey = methodKeyOf(name, methodDescriptor);
                if (classInfo.getMethods().contains(methodKey)) {
                    context.queue(new RelocateMethod(ownerInternalName, methodKey));
                    break;
                }
            }

            classInfo.getMethods().stream()
                .filter(methodKey -> methodKey.getName().equals(name))
                .forEach(methodKey -> context.queue(new RelocateMethod(ownerInternalName, methodKey)));
        } while (false);

        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

}
