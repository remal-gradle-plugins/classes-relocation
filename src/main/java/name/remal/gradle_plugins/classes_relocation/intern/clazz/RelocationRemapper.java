package name.remal.gradle_plugins.classes_relocation.intern.clazz;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;

import java.util.Set;
import lombok.Builder;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.state.RelocationState;
import org.objectweb.asm.commons.Remapper;

public class RelocationRemapper extends Remapper {

    private final RelocationState state;
    private final Set<String> relocationInternalClassNames;
    private final String relocatedClassNamePrefix;
    private final String relocatedClassInternalNamePrefix;

    @Builder
    private RelocationRemapper(
        RelocationState state,
        Set<String> relocationInternalClassNames,
        String basePackageForRelocatedClasses
    ) {
        this.state = state;
        this.relocationInternalClassNames = relocationInternalClassNames;
        this.relocatedClassNamePrefix = basePackageForRelocatedClasses + '.';
        this.relocatedClassInternalNamePrefix = toClassInternalName(relocatedClassNamePrefix);
    }

    @Override
    public String mapType(String internalName) {
        if (!relocationInternalClassNames.contains(internalName)) {
            return internalName;
        }

        val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
        state.relocateClass(relocatedInternalName);
        return relocatedInternalName;
    }

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            val string = (String) value;

            {
                if (relocationInternalClassNames.contains(string)) {
                    val relocatedInternalName = relocatedClassInternalNamePrefix + string;
                    state.relocateClass(relocatedInternalName);
                    return relocatedInternalName;
                }
            }

            {
                val internalName = toClassInternalName(string);
                if (toClassName(internalName).equals(string)
                    && relocationInternalClassNames.contains(internalName)
                ) {
                    val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
                    state.relocateClass(relocatedInternalName);
                    val relocatedName = relocatedClassNamePrefix + string;
                    return relocatedName;
                }
            }
        }

        return super.mapValue(value);
    }

}
