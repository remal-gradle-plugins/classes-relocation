package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.toolkit.GlobPattern;

@NoArgsConstructor
@ToString
@EqualsAndHashCode
public final class ResourcesFilter {

    private final Collection<GlobPattern> inclusions = new ArrayList<>();

    private final Collection<GlobPattern> exclusions = new ArrayList<>();

    public boolean matches(@Nullable String resourceName) {
        if (resourceName == null) {
            return false;
        }

        return matches(inclusions, resourceName, true)
            && !matches(exclusions, resourceName, false);
    }

    private boolean matches(Collection<GlobPattern> patterns, String resourceName, boolean emptyValue) {
        if (patterns.isEmpty()) {
            return emptyValue;
        }

        for (var pattern : patterns) {
            if (pattern.matches(resourceName)) {
                return true;
            }
        }

        return false;
    }

    public boolean matches(@Nullable Resource resource) {
        if (resource == null) {
            return false;
        }

        return matches(resource.getName());
    }


    @CanIgnoreReturnValue
    public ResourcesFilter include(Iterable<String> includes) {
        StreamSupport.stream(includes.spliterator(), false)
            .filter(Objects::nonNull)
            .map(GlobPattern::compile)
            .filter(not(inclusions::contains))
            .forEach(inclusions::add);
        return this;
    }

    @CanIgnoreReturnValue
    public ResourcesFilter include(String... includes) {
        return include(List.of(includes));
    }

    @CanIgnoreReturnValue
    public ResourcesFilter exclude(Iterable<String> excludes) {
        StreamSupport.stream(excludes.spliterator(), false)
            .filter(Objects::nonNull)
            .map(GlobPattern::compile)
            .filter(not(exclusions::contains))
            .forEach(exclusions::add);
        return this;
    }

    @CanIgnoreReturnValue
    public ResourcesFilter exclude(String... excludes) {
        return exclude(List.of(excludes));
    }


    @CanIgnoreReturnValue
    public ResourcesFilter includeClasses(Iterable<String> classNames) {
        return include(StreamSupport.stream(classNames.spliterator(), false)
            .map(name -> toClassInternalName(name) + ".class")
            .collect(toList())
        );
    }

    @CanIgnoreReturnValue
    public ResourcesFilter includeClass(String... classNames) {
        return includeClasses(List.of(classNames));
    }

    @CanIgnoreReturnValue
    public ResourcesFilter excludeClasses(Iterable<String> classNames) {
        return exclude(StreamSupport.stream(classNames.spliterator(), false)
            .map(name -> toClassInternalName(name) + ".class")
            .collect(toList())
        );
    }

    @CanIgnoreReturnValue
    public ResourcesFilter excludeClass(String... classNames) {
        return excludeClasses(List.of(classNames));
    }


    @CanIgnoreReturnValue
    public ResourcesFilter includePackages(Iterable<String> packageNames) {
        return include(StreamSupport.stream(packageNames.spliterator(), false)
            .map(name -> toClassInternalName(name) + "/*")
            .collect(toList())
        );
    }

    @CanIgnoreReturnValue
    public ResourcesFilter includePackage(String... packageNames) {
        return includePackages(List.of(packageNames));
    }

    @CanIgnoreReturnValue
    public ResourcesFilter excludePackages(Iterable<String> packageNames) {
        return exclude(StreamSupport.stream(packageNames.spliterator(), false)
            .map(name -> toClassInternalName(name) + "/*")
            .collect(toList())
        );
    }

    @CanIgnoreReturnValue
    public ResourcesFilter excludePackage(String... packageNames) {
        return excludePackages(List.of(packageNames));
    }


    @CanIgnoreReturnValue
    public ResourcesFilter copyFrom(ResourcesFilter other) {
        other.inclusions.stream()
            .filter(not(inclusions::contains))
            .forEach(inclusions::add);
        other.exclusions.stream()
            .filter(not(exclusions::contains))
            .forEach(exclusions::add);
        return this;
    }


    @CanIgnoreReturnValue
    public ResourcesFilter negate() {
        if (isEmpty()) {
            exclude("**/*");

        } else {
            var inclusions = new ArrayList<>(this.inclusions);
            var exclusions = new ArrayList<>(this.exclusions);

            this.inclusions.clear();
            this.inclusions.addAll(exclusions);

            this.exclusions.clear();
            this.exclusions.addAll(inclusions);
        }

        return this;
    }


    public boolean isEmpty() {
        return inclusions.isEmpty() && exclusions.isEmpty();
    }

}
