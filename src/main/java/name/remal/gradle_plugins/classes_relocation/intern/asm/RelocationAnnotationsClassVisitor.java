package name.remal.gradle_plugins.classes_relocation.intern.asm;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.getLatestAsmApi;
import static org.objectweb.asm.Type.getDescriptor;

import javax.annotation.Nullable;
import lombok.val;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

public class RelocationAnnotationsClassVisitor extends ClassVisitor {

    private static final String GENERATED_DESCRIPTOR = getDescriptor(Generated.class);
    private static final String RELOCATED_CLASS_DESCRIPTOR = getDescriptor(RelocatedClass.class);

    public RelocationAnnotationsClassVisitor(ClassVisitor classVisitor) {
        super(getLatestAsmApi(), classVisitor);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        @Nullable String signature,
        @Nullable String superName,
        @Nullable String[] interfaces
    ) {
        super.visit(version, access, name, signature, superName, interfaces);

        {
            val an = cv.visitAnnotation(GENERATED_DESCRIPTOR, false);
            if (an != null) {
                an.visitEnd();
            }
        }

        {
            val an = cv.visitAnnotation(RELOCATED_CLASS_DESCRIPTOR, false);
            if (an != null) {
                an.visitEnd();
            }
        }
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (GENERATED_DESCRIPTOR.equals(descriptor)
            || RELOCATED_CLASS_DESCRIPTOR.equals(descriptor)
        ) {
            return null;
        }

        return super.visitAnnotation(descriptor, visible);
    }

}
