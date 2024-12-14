package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

public class GeneratedResource extends ResourceBase {

    public static GeneratedResource newGeneratedResource(
        String name,
        byte[] content
    ) {
        return newGeneratedResource(name, null, null, content);
    }

    public static GeneratedResource newGeneratedResource(
        String name,
        @Nullable Integer multiReleaseVersion,
        @Nullable Long lastModifiedMillis,
        byte[] content
    ) {
        return new GeneratedResource(
            name,
            multiReleaseVersion,
            lastModifiedMillis,
            content,
            null
        );
    }

    public static GeneratedResource newGeneratedResource(
        Collection<? extends Resource> sourceResources,
        byte[] content
    ) {
        val resourceKey = getUniqueResourceKey(sourceResources);
        return new GeneratedResource(
            resourceKey.getName(),
            resourceKey.getMultiReleaseVersion(),
            getLastModifiedMillis(sourceResources),
            content,
            sourceResources
        );
    }

    public static GeneratedResource newGeneratedResource(
        Collection<? extends Resource> sourceResources,
        String updatedName,
        byte[] content
    ) {
        val resourceKey = getUniqueResourceKey(sourceResources);
        return new GeneratedResource(
            updatedName,
            resourceKey.getMultiReleaseVersion(),
            getLastModifiedMillis(sourceResources),
            content,
            sourceResources
        );
    }

    private static ResourceKey getUniqueResourceKey(Collection<? extends Resource> sourceResources) {
        val resourceKeys = sourceResources.stream()
            .map(ResourceKey::resourceKeyFor)
            .distinct()
            .collect(toList());
        if (resourceKeys.isEmpty()) {
            throw new IllegalArgumentException("Source resources must not be empty");
        } else if (resourceKeys.size() > 1) {
            throw new IllegalArgumentException("Source resources have different resource keys: " + resourceKeys);
        }
        return resourceKeys.get(0);
    }

    @Nullable
    private static Long getLastModifiedMillis(Collection<? extends Resource> sourceResources) {
        return sourceResources.stream()
            .map(Resource::getLastModifiedMillis)
            .max(Long::compareTo)
            .orElse(null);
    }


    @Getter
    private final long lastModifiedMillis;

    private final byte[] content;

    @Getter
    @Unmodifiable
    private final List<Resource> sourceResources;

    protected GeneratedResource(
        String name,
        @Nullable Integer multiReleaseVersion,
        @Nullable Long lastModifiedMillis,
        byte[] content,
        @Nullable Collection<? extends Resource> sourceResources
    ) {
        super(name, multiReleaseVersion);
        this.lastModifiedMillis = lastModifiedMillis != null && lastModifiedMillis >= 0
            ? lastModifiedMillis
            : currentTimeMillis();
        this.content = content.clone();
        this.sourceResources = sourceResources != null ? ImmutableList.copyOf(sourceResources) : ImmutableList.of();
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(content);
    }

    @Nullable
    @Override
    public ClasspathElement getClasspathElement() {
        return null;
    }

}
