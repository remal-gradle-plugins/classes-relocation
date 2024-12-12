package name.remal.gradle_plugins.classes_relocation.intern;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.withoutMultiReleasePathPrefix;

import java.util.Collection;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceHandler;
import name.remal.gradle_plugins.toolkit.GlobPattern;

abstract class ResourcesHandlerWithCompiledPatterns<T extends ResourceHandler> {

    protected final T handler;

    private final Collection<GlobPattern> inclusions;
    private final Collection<GlobPattern> exclusions;

    protected ResourcesHandlerWithCompiledPatterns(T handler) {
        this.handler = handler;
        this.inclusions = handler.getInclusions().stream()
            .distinct()
            .map(GlobPattern::compile)
            .collect(toImmutableList());
        this.exclusions = handler.getExclusions().stream()
            .distinct()
            .map(GlobPattern::compile)
            .collect(toImmutableList());
    }

    public boolean matches(String resourceName) {
        resourceName = withoutMultiReleasePathPrefix(resourceName);

        if (!inclusions.isEmpty()) {
            for (val pattern : inclusions) {
                if (!pattern.matches(resourceName)) {
                    return false;
                }
            }
        }

        if (!exclusions.isEmpty()) {
            for (val pattern : exclusions) {
                if (pattern.matches(resourceName)) {
                    return false;
                }
            }
        }

        return true;
    }

}
