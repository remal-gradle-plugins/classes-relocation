package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.val;
import name.remal.gradle_plugins.toolkit.GlobPattern;

class ClasspathElementFiltered extends ClasspathElementBase {

    private final ClasspathElement delegate;
    private final ResourcesFilter filter;

    public ClasspathElementFiltered(ClasspathElement delegate, ResourcesFilter filter) {
        super(delegate.getPath());
        this.delegate = closables.registerCloseable(delegate);
        this.filter = filter;
    }

    @Override
    public boolean isMultiRelease() {
        return delegate.isMultiRelease();
    }

    @Override
    protected Collection<Resource> readClasspathElementResources() {
        val inclusions = processPatterns(filter.getInclusions());
        val exclusions = processPatterns(filter.getExclusions());
        return delegate.getResources().stream()
            .filter(resource -> {
                if (inclusions.isEmpty()) {
                    return true;
                }

                for (val inclusion : inclusions) {
                    if (inclusion.matches(resource.getName())) {
                        return true;
                    }
                }
                return false;
            })
            .filter(resource -> {
                if (exclusions.isEmpty()) {
                    return true;
                }

                for (val exclusion : exclusions) {
                    if (exclusion.matches(resource.getName())) {
                        return false;
                    }
                }
                return true;
            })
            .collect(toImmutableList());
    }

    private List<GlobPattern> processPatterns(Collection<String> patterns) {
        if (patterns.isEmpty()) {
            return ImmutableList.of();
        }

        val multiRelease = isMultiRelease();
        return patterns.stream()
            .flatMap(pattern -> {
                if (multiRelease) {
                    return Stream.of(
                        pattern,
                        "META-INF/versions/*/" + pattern
                    );
                }
                return Stream.of(pattern);
            })
            .map(GlobPattern::compile)
            .collect(toImmutableList());
    }

}
