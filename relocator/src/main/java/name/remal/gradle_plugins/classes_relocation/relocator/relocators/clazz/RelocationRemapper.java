package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ProcessStringConstant;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant.ProcessTypeConstant;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

@RequiredArgsConstructor
class RelocationRemapper extends Remapper {

    private final String classInternalName;
    private final Resource classResource;
    private final RelocationContext context;


    @Override
    public String map(String internalName) {
        if (context.isRelocationClassInternalName(internalName)) {
            context.queue(new RelocateClass(internalName));
            return context.getRelocatedClassInternalNamePrefix() + internalName;
        }

        return internalName;
    }

    private final Map<String, String> mappedStrings = new LinkedHashMap<>();

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            val string = (String) value;
            value = mappedStrings.computeIfAbsent(string, str ->
                context.execute(new ProcessStringConstant(classResource, classInternalName, str), str)
            );
        }

        if (value instanceof Type) {
            val type = (Type) value;
            value = context.execute(new ProcessTypeConstant(type), type);
        }

        return super.mapValue(value);
    }

}
