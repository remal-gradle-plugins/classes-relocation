package name.remal.gradle_plugins.classes_relocation.intern.utils;

import static java.lang.Integer.parseInt;
import static lombok.AccessLevel.PRIVATE;

import java.util.jar.Attributes.Name;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

@NoArgsConstructor(access = PRIVATE)
public abstract class MultiReleaseUtils {

    public static final Name MULTI_RELEASE = new Name("Multi-Release");


    private static final Pattern MULTI_RELEASE_RESOURCE_NAME = Pattern.compile(
        "^META-INF/versions/(\\d+)/(.+)$"
    );

    public static boolean isMultiRelease(String resourceName) {
        return MULTI_RELEASE_RESOURCE_NAME.matcher(resourceName).matches();
    }

    public static boolean isMultiRelease(Resource resource) {
        return isMultiRelease(resource.getName());
    }

    @Nullable
    public static Integer getMultiReleaseVersion(String resourceName) {
        val matcher = MULTI_RELEASE_RESOURCE_NAME.matcher(resourceName);
        if (matcher.matches()) {
            return parseInt(matcher.group(1));
        }
        return null;
    }

    public static String withMultiReleasePathPrefix(String resourceName, @Nullable Integer version) {
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


    public static MultiReleaseResourceName parseMultiReleaseResourceName(String resourceName) {
        val matcher = MULTI_RELEASE_RESOURCE_NAME.matcher(resourceName);
        if (matcher.matches()) {
            return new MultiReleaseResourceName(
                matcher.group(2),
                parseInt(matcher.group(1))
            );
        }

        return new MultiReleaseResourceName(
            resourceName,
            null
        );
    }

    @Value
    @RequiredArgsConstructor(access = PRIVATE)
    public static class MultiReleaseResourceName {

        String name;

        @Nullable
        Integer multiReleaseVersion;

    }

}
