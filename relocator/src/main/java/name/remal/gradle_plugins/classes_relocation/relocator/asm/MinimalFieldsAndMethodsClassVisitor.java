package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import javax.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class MinimalFieldsAndMethodsClassVisitor extends ClassVisitor {

    public MinimalFieldsAndMethodsClassVisitor(@Nullable ClassVisitor classVisitor) {
        super(getLatestAsmApi(), classVisitor);
    }

    @Override
    @Nullable
    public FieldVisitor visitField(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable Object value
    ) {
        return null;
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions
    ) {
        if (isStaticInitializerMethod(access, name, descriptor)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        return null;
    }

    private static boolean isStaticInitializerMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) != 0
            && name.equals("<clinit>")
            && descriptor.equals("()V");
    }

}
