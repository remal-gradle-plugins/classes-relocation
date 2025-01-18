package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static org.objectweb.asm.Opcodes.H_PUTSTATIC;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant.ProcessStringConstant;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant.ProcessTypeConstant;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

@RequiredArgsConstructor
@CustomLog
class RelocationRemapper extends Remapper {

    private final String classInternalName;
    private final Resource classResource;
    private final RelocationContext context;

    @Override
    public String map(String internalName) {
        if (context.isRelocationClassInternalName(internalName)) {
            if (!classInternalName.equals(internalName)) {
                context.queue(new RelocateClass(internalName));
            }
            return context.getRelocatedClassInternalNamePrefix() + internalName;
        }

        return internalName;
    }

    private final Map<String, String> mappedStrings = new LinkedHashMap<>();
    private final Map<Type, Type> mappedTypes = new LinkedHashMap<>();

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            var string = (String) value;
            value = mappedStrings.computeIfAbsent(string, str ->
                context.execute(new ProcessStringConstant(str, classInternalName, classResource), str)
            );
        }

        if (value instanceof Type) {
            var type = (Type) value;
            value = mappedTypes.computeIfAbsent(type, curType ->
                context.execute(new ProcessTypeConstant(curType, classResource), curType)
            );
        }

        if (value instanceof Handle) {
            var handle = (Handle) value;
            if (context.isRelocationClassInternalName(handle.getOwner())) {
                var isFieldHandle = handle.getTag() <= H_PUTSTATIC;
                if (isFieldHandle) {
                    context.queue(new RelocateField(handle.getOwner(), handle.getName()));
                } else {
                    context.queue(new RelocateMethod(handle.getOwner(), handle.getName(), handle.getDesc()));
                }
            }
        }

        return super.mapValue(value);
    }

}
