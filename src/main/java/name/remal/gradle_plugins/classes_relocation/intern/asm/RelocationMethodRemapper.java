package name.remal.gradle_plugins.classes_relocation.intern.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

public class RelocationMethodRemapper extends MethodRemapper {

    public RelocationMethodRemapper(int api, MethodVisitor methodVisitor, Remapper remapper) {
        super(api, methodVisitor, remapper);
    }

}
