package name.remal.gradle_plugins.classes_relocation.intern.asm;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;

import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.objectweb.asm.commons.Remapper;

@RequiredArgsConstructor
public class RelocationRemapper extends Remapper {

    private final String relocatedClassInternalNamePrefix;
    private final Predicate<String> isRelocationClassInternalName;
    private final Consumer<String> relocatedClassInternalNameConsumer;

    @Override
    public String map(String internalName) {
        if (isRelocationClassInternalName.test(internalName)) {
            val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
            relocatedClassInternalNameConsumer.accept(relocatedInternalName);
            return relocatedInternalName;
        }

        return internalName;
    }

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            val string = (String) value;
            if (string.contains(".")) {
                val internalName = toClassInternalName(string);
                if (isRelocationClassInternalName.test(internalName)) {
                    val name = toClassName(internalName);
                    if (name.equals(string)) {
                        val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
                        relocatedClassInternalNameConsumer.accept(relocatedInternalName);
                        val relocatedName = toClassName(relocatedInternalName);
                        return relocatedName;
                    }
                }
            }
        }

        return super.mapValue(value);
    }

}
