package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getClassDescriptor;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static org.jetbrains.annotations.ApiStatus.Internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import lombok.val;
import org.apiguardian.api.API;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

public class RelocationAnnotationsClassVisitor extends ClassVisitor {

    private static final String GENERATED_DESCRIPTOR = getClassDescriptor(Generated.class);

    private static final String RELOCATED_CLASS_DESCRIPTOR = getClassDescriptor(RelocatedClass.class);

    private static final String JETBRAINS_INTERNAL_DESCRIPTOR = getClassDescriptor(Internal.class);

    private static final String APIGUARDIAN_API_DESCRIPTOR = getClassDescriptor(API.class);

    private static final String SUPPRESS_FB_WARNINGS_DESCRIPTOR = getClassDescriptor(SuppressFBWarnings.class);


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
            val av = super.visitAnnotation(JETBRAINS_INTERNAL_DESCRIPTOR, false);
            if (av != null) {
                av.visitEnd();
            }
        }

        {
            val av = super.visitAnnotation(APIGUARDIAN_API_DESCRIPTOR, false);
            if (av != null) {
                av.visitEnum("status", getClassDescriptor(API.Status.class), "INTERNAL");
                av.visitEnd();
            }
        }

        {
            val av = super.visitAnnotation(SUPPRESS_FB_WARNINGS_DESCRIPTOR, false);
            if (av != null) {
                av.visit("justification", "relocated class");
                av.visitEnd();
            }
        }
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (JETBRAINS_INTERNAL_DESCRIPTOR.equals(descriptor)
            || APIGUARDIAN_API_DESCRIPTOR.equals(descriptor)
            || GENERATED_DESCRIPTOR.equals(descriptor)
            || RELOCATED_CLASS_DESCRIPTOR.equals(descriptor)
            || SUPPRESS_FB_WARNINGS_DESCRIPTOR.equals(descriptor)
        ) {
            return null;
        }

        return super.visitAnnotation(descriptor, visible);
    }

}
