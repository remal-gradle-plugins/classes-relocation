package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static org.objectweb.asm.Type.getType;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.val;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

class FilterMethodVisitor extends MethodVisitor {

    private final Predicate<String> classInternalNamePredicate;

    public FilterMethodVisitor(
        int api,
        @Nullable MethodVisitor methodVisitor,
        Predicate<String> classInternalNamePredicate
    ) {
        super(api, methodVisitor);
        this.classInternalNamePredicate = classInternalNamePredicate;
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

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
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitInsnAnnotation(
        int typeRef,
        @Nullable TypePath typePath,
        String descriptor,
        boolean visible
    ) {
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

        return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitLocalVariableAnnotation(
        int typeRef,
        @Nullable TypePath typePath,
        Label[] start,
        Label[] end,
        int[] index,
        String descriptor,
        boolean visible
    ) {
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

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
        val internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

        return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }

}
