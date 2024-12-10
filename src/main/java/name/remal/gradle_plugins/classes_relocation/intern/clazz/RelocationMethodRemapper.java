package name.remal.gradle_plugins.classes_relocation.intern.clazz;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import javax.annotation.Nullable;
import lombok.Builder;
import name.remal.gradle_plugins.classes_relocation.intern.state.RelocationState;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

public class RelocationMethodRemapper extends MethodRemapper {

    private final RelocationState state;

    @Builder
    private RelocationMethodRemapper(
        RelocationState state,
        int api,
        MethodVisitor methodVisitor,
        Remapper remapper
    ) {
        super(api, methodVisitor, remapper);
        this.state = state;
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lname/remal/gradle_plugins/api/RelocateClasses;")
            || descriptor.equals("Lname/remal/gradle_plugins/api/RelocatePackages;")
        ) {
            throw new UnsupportedOperationException("Not supported: " + descriptor);
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        state.registerField(owner, name);
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
        state.registerMethod(
            owner,
            (opcodeAndSource & INVOKESTATIC) == 0,
            name,
            descriptor
        );
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }

}
