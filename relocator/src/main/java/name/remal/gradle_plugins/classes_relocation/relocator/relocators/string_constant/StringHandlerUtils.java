package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
abstract class StringHandlerUtils {

    public static boolean isClassName(String string) {
        return isIdentifier(string, '.');
    }

    public static boolean isClassInternalName(String string) {
        return isIdentifier(string, '/');
    }

    @SuppressWarnings("java:S3776")
    private static boolean isIdentifier(String string, char delimiter) {
        if (string.isEmpty()) {
            return false;
        }

        if (string.charAt(string.length() - 1) == delimiter) {
            return false;
        }

        int startPos = 0;
        while (startPos < string.length()) {
            var delimPos = string.indexOf(delimiter, startPos);
            var endPos = delimPos >= 0 ? delimPos : string.length();
            if (startPos >= endPos) {
                return false;
            }

            var firstCh = string.charAt(startPos);
            if (!isJavaIdentifierStart(firstCh)) {
                return false;
            }

            for (int i = startPos + 1; i < endPos; i++) {
                var ch = string.charAt(i);
                if (!isJavaIdentifierPart(ch)) {
                    return false;
                }
            }

            startPos = endPos + 1;
        }

        return true;
    }

}
