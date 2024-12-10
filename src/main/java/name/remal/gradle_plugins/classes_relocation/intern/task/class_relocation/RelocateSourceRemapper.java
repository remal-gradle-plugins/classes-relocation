package name.remal.gradle_plugins.classes_relocation.intern.task.class_relocation;

import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import org.objectweb.asm.commons.Remapper;

class RelocateSourceRemapper extends Remapper {

    private final RelocationContext context;

    public RelocateSourceRemapper(RelocationContext context) {
        this.context = context;
    }

    @Override
    public Object mapValue(@Nullable Object value) {
        if (value instanceof String) {
            val string = (String) value;
            val relocatedClassInternalName = context.relocateClassLiteral(string);
            if (!relocatedClassInternalName.equals(string)) {
                return relocatedClassInternalName;
            }

            val relocatedResourcePath = context.relocateResource(string);
            if (!relocatedResourcePath.equals(string)) {
                return relocatedResourcePath;
            }

            return string;
        }

        return super.mapValue(value);
    }

    @Override
    public String map(String internalName) {
        return context.relocateClassLiteral(internalName);
    }

}
