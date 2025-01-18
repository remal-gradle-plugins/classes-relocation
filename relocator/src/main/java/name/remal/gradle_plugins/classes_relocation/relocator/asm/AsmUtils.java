package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@NoArgsConstructor(access = PRIVATE)
public abstract class AsmUtils {

    private static final Pattern ASM_API_FIELD_NAME = Pattern.compile("^ASM(\\d+)$");

    private static final LazyValue<Integer> LATEST_ASM_API = lazyValue(() -> {
        val latestAsmApiField = stream(Opcodes.class.getFields())
            .filter(ReflectionUtils::isStatic)
            .filter(ReflectionUtils::isNotSynthetic)
            .filter(field ->
                field.getType() == int.class
                    && ASM_API_FIELD_NAME.matcher(field.getName()).matches()
            )
            .max(comparingInt(field -> {
                val matcher = ASM_API_FIELD_NAME.matcher(field.getName());
                if (!matcher.matches()) {
                    throw new AssertionError("unreachable");
                }

                val apiStr = matcher.group(1);
                val apiVersion = parseInt(apiStr);
                return apiVersion;
            }))
            .orElseThrow(() -> new IllegalStateException("Latest ASM API field not found"));

        return (Integer) latestAsmApiField.get(null);
    });

    public static int getLatestAsmApi() {
        return LATEST_ASM_API.get();
    }


    public static String toClassInternalName(Class<?> clazz) {
        return toClassInternalName(clazz.getName());
    }

    public static String toClassInternalName(String classNameOrInternalName) {
        return classNameOrInternalName.replace('.', '/');
    }

    public static String toClassName(String classNameOrInternalName) {
        return classNameOrInternalName.replace('/', '.');
    }

    public static String toMethodParamsDescriptor(String methodDescriptorOrParamsDescriptor) {
        val paramDescEndPos = methodDescriptorOrParamsDescriptor.lastIndexOf(')');
        return paramDescEndPos >= 0
            ? methodDescriptorOrParamsDescriptor.substring(0, paramDescEndPos + 1)
            : methodDescriptorOrParamsDescriptor;
    }


    public static boolean isBridgeMethodOf(
        MethodNode checkedMethod,
        String targetClassInnerName,
        String targetMethodName,
        String targetMethodDescriptor
    ) {
        if ((checkedMethod.access & ACC_BRIDGE) == 0
            || (checkedMethod.access & ACC_STATIC) != 0
            || !checkedMethod.name.equals(targetMethodName)
        ) {
            return false;
        }

        return StreamSupport.stream(checkedMethod.instructions.spliterator(), false)
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(insn -> insn.getOpcode() == INVOKEVIRTUAL
                && insn.owner.equals(targetClassInnerName)
                && insn.name.equals(targetMethodName)
                && insn.name.equals(targetMethodDescriptor)
            );
    }

    public static boolean isBridgeMethodOf(
        MethodNode checkedMethod,
        String targetClassInnerName,
        MethodNode targetMethod
    ) {
        if ((targetMethod.access & ACC_STATIC) != 0) {
            return false;
        }

        return isBridgeMethodOf(checkedMethod, targetClassInnerName, targetMethod.name, targetMethod.desc);
    }

}
