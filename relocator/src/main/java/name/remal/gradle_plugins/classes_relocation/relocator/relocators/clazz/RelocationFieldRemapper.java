package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.Remapper;

class RelocationFieldRemapper extends FieldRemapper {

    public RelocationFieldRemapper(
        int api,
        FieldVisitor fieldVisitor,
        Remapper remapper
    ) {
        super(api, fieldVisitor, remapper);
    }

}
