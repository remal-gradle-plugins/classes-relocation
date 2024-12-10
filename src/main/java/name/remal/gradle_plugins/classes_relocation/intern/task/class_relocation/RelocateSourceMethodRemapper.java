package name.remal.gradle_plugins.classes_relocation.intern.task.class_relocation;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

class RelocateSourceMethodRemapper extends MethodRemapper {

    private final RelocationContext context;

    public RelocateSourceMethodRemapper(RelocationContext context, MethodVisitor methodVisitor, Remapper remapper) {
        super(methodVisitor, remapper);
        this.context = context;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        context.relocateField(owner, name, descriptor);
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
        context.relocateMethod(owner, name, descriptor);
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }

}
