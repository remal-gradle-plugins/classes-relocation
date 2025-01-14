package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PACKAGE;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Contract;

@NoArgsConstructor(access = PACKAGE)
public final class GeneratedResourceBuilder {

    private static final LazyValue<byte[]> EMPTY_CONTENT = lazyValue(() -> new byte[0]);


    private ImmutableList<Resource> sourceResources = ImmutableList.of();

    @Nullable
    private String name;

    @Nullable
    private Integer multiReleaseVersion = -1;

    @Nullable
    private Long lastModifiedMillis;

    @Nullable
    private LazyValue<byte[]> content;


    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withSourceResource(Resource sourceResource) {
        this.sourceResources = ImmutableList.of(sourceResource);
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withSourceResources(Collection<? extends Resource> sourceResources) {
        this.sourceResources = ImmutableList.copyOf(sourceResources);
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withMultiReleaseVersion(@Nullable Integer multiReleaseVersion) {
        if (multiReleaseVersion != null && multiReleaseVersion <= 0) {
            throw new IllegalArgumentException("Multi-Release version must be positive");
        }
        this.multiReleaseVersion = multiReleaseVersion;
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("->this")
    public GeneratedResourceBuilder withoutMultiReleaseVersion() {
        return withMultiReleaseVersion(null);
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withLastModifiedMillis(long lastModifiedMillis) {
        this.lastModifiedMillis = lastModifiedMillis;
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withContent(byte[] content) {
        val clonedContent = content.clone();
        this.content = lazyValue(() -> clonedContent);
        return this;
    }

    @CanIgnoreReturnValue
    @Contract("_->this")
    public GeneratedResourceBuilder withContent(ContentGenerator contentGenerator) {
        this.content = lazyValue(contentGenerator::generate);
        return this;
    }

    @FunctionalInterface
    public interface ContentGenerator {
        byte[] generate() throws Throwable;
    }

    @CanIgnoreReturnValue
    @Contract("->this")
    public GeneratedResourceBuilder withEmptyContent() {
        this.content = EMPTY_CONTENT;
        return this;
    }


    @SuppressWarnings("java:S3776")
    public GeneratedResource build() {
        val withoutName = name == null;
        val withoutMultiReleaseVersion = multiReleaseVersion != null && multiReleaseVersion <= 0;
        if (withoutName || withoutMultiReleaseVersion) {
            if (sourceResources.isEmpty()) {
                throw new IllegalStateException(
                    "Name and Multi-Release version must be set if sources resources are empty"
                );

            } else if (sourceResources.size() == 1) {
                val sourceResource = sourceResources.get(0);
                if (withoutName) {
                    name = sourceResource.getName();
                }
                if (withoutMultiReleaseVersion) {
                    multiReleaseVersion = sourceResource.getMultiReleaseVersion();
                }

            } else {
                val resourceKey = getUniqueResourceKey(sourceResources);
                if (withoutName) {
                    name = resourceKey.getName();
                }
                if (withoutMultiReleaseVersion) {
                    multiReleaseVersion = resourceKey.getMultiReleaseVersion();
                }
            }
        }

        if (lastModifiedMillis == null) {
            lastModifiedMillis = sourceResources.stream()
                .map(Resource::getLastModifiedMillis)
                .max(Long::compareTo)
                .orElse(null);
        }
        if (lastModifiedMillis == null) {
            lastModifiedMillis = currentTimeMillis();
        }

        if (content == null) {
            if (sourceResources.isEmpty()) {
                throw new IllegalStateException(
                    "Content must be set if sources resources are empty"
                );
            } else if (sourceResources.size() == 1) {
                val sourceResource = sourceResources.get(0);
                content = lazyValue(sourceResource::readAllBytes);
            } else {
                throw new IllegalStateException(
                    "Content must be set if multiple sources resources are set"
                );
            }
        }

        return new GeneratedResource(
            sourceResources,
            name,
            multiReleaseVersion,
            lastModifiedMillis,
            content
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

}
