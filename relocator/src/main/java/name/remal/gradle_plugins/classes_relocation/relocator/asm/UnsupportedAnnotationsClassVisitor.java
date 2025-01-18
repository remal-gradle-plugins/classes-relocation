package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static java.util.Arrays.asList;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.val;
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
        this(classVisitor, asList(prohibitedAnnotationDescriptors));
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
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        checkProhibitedAnnotationDescriptor(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
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
    public FieldVisitor visitField(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable Object value
    ) {
        val visitor = super.visitField(access, name, descriptor, signature, value);
        return new FieldVisitor(api, visitor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
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
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions
    ) {
        val visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, visitor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
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
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitParameterAnnotation(parameter, descriptor, visible);
            }

            @Override
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
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, @Nullable String signature) {
        val visitor = super.visitRecordComponent(name, descriptor, signature);
        return new RecordComponentVisitor(api, visitor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                checkProhibitedAnnotationDescriptor(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
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
