package name.remal.gradle_plugins.classes_relocation.relocator.resource;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.errorprone.annotations.ForOverride;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.toolkit.GlobPattern;

public abstract class BaseResourcesHandler implements ResourcesSelector, ResourceProcessor {

    @ForOverride
    protected Optional<Resource> selectImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        List<Resource> candidateResources,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) throws Throwable {
        return Optional.empty();
    }

    @ForOverride
    protected Optional<Resource> processResourceImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) throws Throwable {
        return Optional.empty();
    }


    private final List<GlobPattern> inclusions;
    private final List<GlobPattern> exclusions;

    protected BaseResourcesHandler(Collection<String> inclusions, Collection<String> exclusions) {
        this.inclusions = inclusions.stream()
            .filter(Objects::nonNull)
            .map(GlobPattern::compile)
            .distinct()
            .collect(toImmutableList());
        this.exclusions = exclusions.stream()
            .filter(Objects::nonNull)
            .map(GlobPattern::compile)
            .distinct()
            .collect(toImmutableList());
    }

    private boolean isIgnored(String resourceName) {
        if (!inclusions.isEmpty()) {
            if (inclusions.stream().noneMatch(pattern -> pattern.matches(resourceName))) {
                return true;
            }
        }
        if (!exclusions.isEmpty()) {
            if (exclusions.stream().anyMatch(pattern -> pattern.matches(resourceName))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Optional<Resource> select(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        List<Resource> candidateResources,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) throws Throwable {
        if (isIgnored(originalResourceName)) {
            return Optional.empty();
        }

        return selectImpl(
            resourceName,
            originalResourceName,
            multiReleaseVersion,
            candidateResources,
            classpathElement,
            context
        );
    }

    @Override
    public final Optional<Resource> processResource(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) throws Throwable {
        if (isIgnored(originalResourceName)) {
            return Optional.empty();
        }

        return processResourceImpl(
            resourceName,
            originalResourceName,
            multiReleaseVersion,
            resource,
            context
        );
    }

}
