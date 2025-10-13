package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.withMultiReleasePathPrefix;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ResourceKey {

    public static ResourceKey resourceKeyFor(Resource resource) {
        return new ResourceKey(resource.getName(), resource.getMultiReleaseVersion());
    }


    String name;

    @Nullable
    Integer multiReleaseVersion;


    @Override
    public String toString() {
        return withMultiReleasePathPrefix(name, multiReleaseVersion);
    }

}
