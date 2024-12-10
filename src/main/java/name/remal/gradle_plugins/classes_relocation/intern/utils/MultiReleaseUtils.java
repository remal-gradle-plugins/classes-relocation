package name.remal.gradle_plugins.classes_relocation.intern.utils;

import static java.lang.Integer.parseInt;
import static lombok.AccessLevel.PRIVATE;

import java.util.jar.Attributes.Name;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public abstract class MultiReleaseUtils {

    public static final Name MULTI_RELEASE_NAME = new Name("Multi-Release");


    private static final Pattern MULTI_RELEASE_RESOURCE_NAME = Pattern.compile(
        "^META-INF/versions/(\\d+)/(.+)$"
    );

    @Nullable
    public static Integer getMultiReleaseVersion(String resourceName) {
        val matcher = MULTI_RELEASE_RESOURCE_NAME.matcher(resourceName);
        if (matcher.matches()) {
            return parseInt(matcher.group(1));
        }
        return null;
    }

    public static String getMultiReleaseResourceName(String resourceName, @Nullable Integer version) {
        if (version == null) {
            return resourceName;
        }
        return "META-INF/versions/" + version + "/" + resourceName;
    }

    public static String withoutMultiReleasePathPrefix(String resourceName) {
        val matcher = MULTI_RELEASE_RESOURCE_NAME.matcher(resourceName);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return resourceName;
    }

}
