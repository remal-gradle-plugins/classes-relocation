package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static lombok.AccessLevel.PRIVATE;

import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ResourceKey {

    public static ResourceKey resourceKeyFor(Resource resource) {
        return new ResourceKey(resource.getName(), resource.getMultiReleaseVersion());
    }


    String name;

    @Nullable
    Integer multiReleaseVersion;

}
