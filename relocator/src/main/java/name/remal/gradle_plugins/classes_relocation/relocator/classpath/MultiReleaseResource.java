package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.Collections.unmodifiableMap;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

@Data
@RequiredArgsConstructor(access = PRIVATE)
public class MultiReleaseResource {

    public static MultiReleaseResource buildMultiReleaseResources(Action<Consumer<Resource>> builder) {
        var sortedResources = new TreeMap<Integer, Resource>();
        builder.execute(resource -> {
            var multiReleaseVersion = defaultValue(resource.getMultiReleaseVersion(), -1);
            sortedResources.putIfAbsent(multiReleaseVersion, resource);
        });

        var resources = new LinkedHashMap<Integer, Resource>();
        sortedResources.forEach((multiReleaseVersion, resource) ->
            resources.putIfAbsent(
                multiReleaseVersion > 0 ? multiReleaseVersion : null,
                resource
            )
        );
        return new MultiReleaseResource(unmodifiableMap(resources));
    }


    @Unmodifiable
    private final Map<@org.jetbrains.annotations.Nullable Integer, Resource> resources;

    @Nullable
    public Resource forReleaseVersion(int releaseVersion) {
        return resources.get(releaseVersion);
    }

}
