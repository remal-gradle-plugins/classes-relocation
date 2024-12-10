package name.remal.gradle_plugins.classes_relocation.intern.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public class RelocationClassRemapper extends ClassRemapper {

    public RelocationClassRemapper(ClassVisitor classVisitor, Remapper remapper) {
        super(classVisitor, remapper);
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        return new RelocationMethodRemapper(api, methodVisitor, remapper);
    }

}
