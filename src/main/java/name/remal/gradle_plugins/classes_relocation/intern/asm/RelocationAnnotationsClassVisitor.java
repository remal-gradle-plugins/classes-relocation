package name.remal.gradle_plugins.classes_relocation.intern.asm;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.getLatestAsmApi;
import static org.objectweb.asm.Type.getDescriptor;

import javax.annotation.Nullable;
import lombok.val;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

public class RelocationAnnotationsClassVisitor extends ClassVisitor {

    private static final String JETBRAINS_INTERNAL_DESCRIPTOR = "Lorg/jetbrains/annotations/ApiStatus$Internal;";

    private static final String GENERATED_DESCRIPTOR = getDescriptor(Generated.class);

    private static final String RELOCATED_CLASS_DESCRIPTOR = getDescriptor(RelocatedClass.class);

    private static final String SUPPRESS_FB_WARNINGS_DESCRIPTOR =
        "Ledu/umd/cs/findbugs/annotations/SuppressFBWarnings;";


    @Nullable
    private final String relocationSource;

    public RelocationAnnotationsClassVisitor(ClassVisitor classVisitor, @Nullable String relocationSource) {
        super(getLatestAsmApi(), classVisitor);
        this.relocationSource = relocationSource;
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
            val av = super.visitAnnotation(JETBRAINS_INTERNAL_DESCRIPTOR, false);
            if (av != null) {
                av.visitEnd();
            }
        }

        {
            val av = super.visitAnnotation(GENERATED_DESCRIPTOR, false);
            if (av != null) {
                av.visitEnd();
            }
        }

        {
            val av = super.visitAnnotation(RELOCATED_CLASS_DESCRIPTOR, false);
            if (av != null) {
                if (relocationSource != null && !relocationSource.isEmpty()) {
                    av.visit("source", relocationSource);
                }
                av.visitEnd();
            }
        }

        {
            val av = super.visitAnnotation(SUPPRESS_FB_WARNINGS_DESCRIPTOR, false);
            if (av != null) {
                av.visitEnd();
            }
        }
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (JETBRAINS_INTERNAL_DESCRIPTOR.equals(descriptor)
            || GENERATED_DESCRIPTOR.equals(descriptor)
            || RELOCATED_CLASS_DESCRIPTOR.equals(descriptor)
            || SUPPRESS_FB_WARNINGS_DESCRIPTOR.equals(descriptor)
        ) {
            return null;
        }

        return super.visitAnnotation(descriptor, visible);
    }

}
