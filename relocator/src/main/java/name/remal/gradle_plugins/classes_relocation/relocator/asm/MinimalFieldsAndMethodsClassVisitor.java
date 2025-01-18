package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class MinimalFieldsAndMethodsClassVisitor extends ClassVisitor {

    private static final String WRITE_OBJECT_METHOD_DESCRIPTOR =
        getMethodDescriptor(VOID_TYPE, getType(ObjectOutputStream.class));

    private static final String READ_OBJECT_METHOD_DESCRIPTOR =
        getMethodDescriptor(VOID_TYPE, getType(ObjectInputStream.class));

    private static final String READ_OBJECT_NO_DATA_METHOD_DESCRIPTOR =
        getMethodDescriptor(VOID_TYPE);

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
        if (isSerialVersionUidField(access, name, descriptor)) {
            return super.visitField(access, name, descriptor, signature, value);
        }

        return null;
    }

    private static boolean isSerialVersionUidField(int access, String name, String descriptor) {
        return (access & ACC_STATIC) != 0
            && (access & ACC_FINAL) != 0
            && name.equals("serialVersionUID")
            && descriptor.equals("J");
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
        if (isStaticInitializerMethod(access, name, descriptor)
            || isWriteObjectMethod(access, name, descriptor)
            || isReadObjectMethod(access, name, descriptor)
            || isReadObjectNoDataMethod(access, name, descriptor)
            || isReadReplaceMethod(access, name, descriptor)
            || isReadResolveMethod(access, name, descriptor)
        ) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        return null;
    }

    private static boolean isStaticInitializerMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) != 0
            && name.equals("<clinit>")
            && descriptor.equals("()V");
    }

    private static boolean isWriteObjectMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) == 0
            && (access & ACC_PRIVATE) != 0
            && name.equals("writeObject")
            && descriptor.equals(WRITE_OBJECT_METHOD_DESCRIPTOR);
    }

    private static boolean isReadObjectMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) == 0
            && (access & ACC_PRIVATE) != 0
            && name.equals("readObject")
            && descriptor.equals(READ_OBJECT_METHOD_DESCRIPTOR);
    }

    private static boolean isReadObjectNoDataMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) == 0
            && (access & ACC_PRIVATE) != 0
            && name.equals("readObjectNoData")
            && descriptor.equals(READ_OBJECT_NO_DATA_METHOD_DESCRIPTOR);
    }

    private static boolean isReadReplaceMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) == 0
            && name.equals("writeReplace")
            && descriptor.startsWith("()");
    }

    private static boolean isReadResolveMethod(int access, String name, String descriptor) {
        return (access & ACC_STATIC) == 0
            && name.equals("readResolve")
            && descriptor.startsWith("()");
    }

}
