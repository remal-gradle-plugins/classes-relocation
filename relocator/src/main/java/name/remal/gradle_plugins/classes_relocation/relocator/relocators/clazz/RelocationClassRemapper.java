package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

class RelocationClassRemapper extends ClassRemapper {

    private final RelocationContext context;

    public RelocationClassRemapper(@Nullable ClassVisitor classVisitor, Remapper remapper, RelocationContext context) {
        super(classVisitor, remapper);
        this.context = context;
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        return new RelocationMethodRemapper(api, methodVisitor, remapper, className, context);
    }

}
