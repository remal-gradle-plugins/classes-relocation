package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

public class UnsupportedAnnotationsClassVisitor extends ClassVisitor {

    @Nullable
    private String className;

    private final Set<String> prohibitedAnnotationDescriptors;

    public UnsupportedAnnotationsClassVisitor(
        ClassVisitor classVisitor,
        Collection<String> prohibitedAnnotationDescriptors
    ) {
        super(getLatestAsmApi(), classVisitor);
        this.prohibitedAnnotationDescriptors = ImmutableSet.copyOf(prohibitedAnnotationDescriptors);
    }

    public UnsupportedAnnotationsClassVisitor(
        ClassVisitor classVisitor,
        String... prohibitedAnnotationDescriptors
    ) {
        this(classVisitor, List.of(prohibitedAnnotationDescriptors));
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
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private UnsupportedAnnotationException newUnsupportedAnnotationException(String descriptor) {
        final String message;
        if (isNotEmpty(className)) {
            message = className + ": unsupported annotation: " + descriptor;
        } else {
            message = "Unsupported annotation: " + descriptor;
        }
        return new UnsupportedAnnotationException(message);
    }

    private void checkProhibitedAnnotationDescriptor(String descriptor) {
        if (prohibitedAnnotationDescriptors.contains(descriptor)) {
            throw newUnsupportedAnnotationException(descriptor);
        }
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        checkProhibitedAnnotationDescriptor(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef,
        @Nullable TypePath typePath,
        String descriptor,
        boolean visible
    ) {
        checkProhibitedAnnotationDescriptor(descriptor);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
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
        var visitor = super.visitField(access, name, descriptor, signature, value);
        return new FieldVisitor(api, visitor) {
            @Override
            @Nullable
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitTypeAnnotation(
                int typeRef,
                @Nullable TypePath typePath,
                String descriptor,
                boolean visible
            ) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
            }
        };
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
        var visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, visitor) {
            @Override
            @Nullable
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitTypeAnnotation(
                int typeRef,
                @Nullable TypePath typePath,
                String descriptor,
                boolean visible
            ) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitParameterAnnotation(parameter, descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitLocalVariableAnnotation(
                int typeRef,
                @Nullable TypePath typePath,
                @Nullable Label[] start,
                @Nullable Label[] end,
                int[] index,
                String descriptor,
                boolean visible
            ) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitTryCatchAnnotation(
                int typeRef,
                @Nullable TypePath typePath,
                String descriptor,
                boolean visible
            ) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
            }
        };
    }

    @Override
    @Nullable
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, @Nullable String signature) {
        var visitor = super.visitRecordComponent(name, descriptor, signature);
        return new RecordComponentVisitor(api, visitor) {
            @Override
            @Nullable
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            @Nullable
            public AnnotationVisitor visitTypeAnnotation(
                int typeRef,
                @Nullable TypePath typePath,
                String descriptor,
                boolean visible
            ) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
            }
        };
    }

}
