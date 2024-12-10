package name.remal.gradle_plugins.classes_relocation.intern.task.class_relocation;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;

class RelocateSourceClassRemapper extends ClassRemapper {

    private final RelocationContext context;

    public RelocateSourceClassRemapper(RelocationContext context, ClassVisitor delegateVisitor) {
        super(delegateVisitor, new RelocateSourceRemapper(context));
        this.context = context;
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        return new RelocateSourceMethodRemapper(context, methodVisitor, remapper);
    }

}
