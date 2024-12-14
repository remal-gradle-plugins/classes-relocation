package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
abstract class StringHandlerUtils {

    public static boolean isClassName(String string) {
        return isIdentifier(string, '.', 0, string.length());
    }

    public static boolean isClassInternalName(String string) {
        return isIdentifier(string, '/', 0, string.length());
    }

    @SuppressWarnings("java:S3776")
    private static boolean isIdentifier(String string, char delimiter, int startPos, int length) {
        while (startPos <= length) {
            val delimPos = string.indexOf(delimiter, startPos);
            val endPos = delimPos >= 0 ? delimPos : length;
            if (startPos >= endPos) {
                return false;
            }

            val firstCh = string.charAt(startPos);
            if (!isJavaIdentifierStart(firstCh)) {
                return false;
            }

            for (int i = startPos + 1; i < endPos; i++) {
                val ch = string.charAt(i);
                if (!isJavaIdentifierPart(ch)) {
                    return false;
                }
            }

            startPos = endPos + 1;
        }

        return true;
    }

}
