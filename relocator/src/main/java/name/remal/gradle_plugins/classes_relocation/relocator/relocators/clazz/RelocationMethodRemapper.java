package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodType;

import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.class_info.ClassInfoComponent;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

class RelocationMethodRemapper extends MethodRemapper {

    private static final Handle DEFAULT_LAMBDA_BOOSTRAP_METHOD_HANDLE = new Handle(
        H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory",
        "metafactory",
        "("
            + "Ljava/lang/invoke/MethodHandles$Lookup"
            + ";Ljava/lang/String"
            + ";Ljava/lang/invoke/MethodType"
            + ";Ljava/lang/invoke/MethodType"
            + ";Ljava/lang/invoke/MethodHandle"
            + ";Ljava/lang/invoke/MethodType"
            + ";"
            + ")Ljava/lang/invoke/CallSite;",
        false
    );


    private final RelocationContext context;

    public RelocationMethodRemapper(
        int api,
        @Nullable MethodVisitor methodVisitor,
        Remapper remapper,
        RelocationContext context
    ) {
        super(api, methodVisitor, remapper);
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
        if (context.isRelocationClassInternalName(owner)) {
            context.queue(new RelocateMethod(owner, name, descriptor));
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