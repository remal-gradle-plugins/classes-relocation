package name.remal.gradle_plugins.classes_relocation.intern.utils;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils;
import org.objectweb.asm.Opcodes;

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
                if (matcher.matches()) {
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


    public static String toClassInternalName(String classNameOrInternalName) {
        return classNameOrInternalName.replace('.', '/');
    }

    public static String toClassName(String classNameOrInternalName) {
        return classNameOrInternalName.replace('/', '.');
    }

    public static String toMethodParamsDescriptor(String methodDescriptor) {
        val paramDescEndPos = methodDescriptor.lastIndexOf(')');
        return paramDescEndPos >= 0
            ? methodDescriptor.substring(0, paramDescEndPos + 1)
            : methodDescriptor;
    }

}
