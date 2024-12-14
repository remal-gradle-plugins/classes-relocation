package name.remal.gradle_plugins.classes_relocation.intern.utils;

import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

@NoArgsConstructor(access = PRIVATE)
public abstract class ResourceNameUtils {

    public static String resourceNameWithRelocationSource(Resource resource, @Nullable String relocationSource) {
        return resourceNameWithRelocationSource(resource.getName(), relocationSource);
    }

    public static String resourceNameWithRelocationSource(String resourceName, @Nullable String relocationSource) {
        if (isEmpty(relocationSource)) {
            return resourceName;
        }

        val delimPos = resourceName.lastIndexOf('/');
        val resourceNamePrefix = delimPos >= 0 ? resourceName.substring(0, delimPos + 1) : "";
        val resourceFileName = resourceName.substring(resourceNamePrefix.length());

        return resourceNamePrefix + escapeRelocationSource(relocationSource) + '-' + resourceFileName;
    }


    private static final char[] FORBIDDEN_RESOURCE_FILE_NAME_CHARS = "\\/:<>\"'|?*${}()&[]^".toCharArray();

    static {
        sort(FORBIDDEN_RESOURCE_FILE_NAME_CHARS);
    }

    private static String escapeRelocationSource(String name) {
        val result = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            val ch = name.charAt(index);
            if (binarySearch(FORBIDDEN_RESOURCE_FILE_NAME_CHARS, ch) >= 0) {
                result.append('-');
            } else if (ch < 32 || ch > 126) {
                result.append('-');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }


}
