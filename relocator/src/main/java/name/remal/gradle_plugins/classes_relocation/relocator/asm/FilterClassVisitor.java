package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static org.objectweb.asm.Type.getType;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

public class FilterClassVisitor extends ClassVisitor {

    private final Predicate<String> classInternalNamePredicate;

    public FilterClassVisitor(
        @Nullable ClassVisitor classVisitor,
        Predicate<String> classInternalNamePredicate
    ) {
        super(getLatestAsmApi(), classVisitor);
        this.classInternalNamePredicate = classInternalNamePredicate;
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        if (!classInternalNamePredicate.test(permittedSubclass)) {
            return;
        }

        super.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public void visitOuterClass(String owner, @Nullable String name, @Nullable String descriptor) {
        if (!classInternalNamePredicate.test(owner)) {
            return;
        }

        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitInnerClass(String name, @Nullable String outerName, @Nullable String innerName, int access) {
        if (!classInternalNamePredicate.test(name)) {
            return;
        }
        if (outerName != null && !classInternalNamePredicate.test(outerName)) {
            return;
        }

        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        var internalName = getType(descriptor).getInternalName();
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
        var internalName = getType(descriptor).getInternalName();
        if (!classInternalNamePredicate.test(internalName)) {
            return null;
        }

        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    @Nullable
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, @Nullable String signature) {
        RecordComponentVisitor visitor = super.visitRecordComponent(name, descriptor, signature);
        if (visitor != null) {
            visitor = new FilterRecordComponentVisitor(api, visitor, classInternalNamePredicate);
        }
        return visitor;
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
        FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
        if (visitor != null) {
            visitor = new FilterFieldVisitor(api, classInternalNamePredicate);
        }
        return visitor;
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
        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (visitor != null) {
            visitor = new FilterMethodVisitor(api, visitor, classInternalNamePredicate);
        }
        return visitor;
    }

}
