package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static org.objectweb.asm.Type.getType;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

class FilterFieldVisitor extends FieldVisitor {

    private final Predicate<String> classInternalNamePredicate;

    public FilterFieldVisitor(int api, Predicate<String> classInternalNamePredicate) {
        super(api);
        this.classInternalNamePredicate = classInternalNamePredicate;
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

}
