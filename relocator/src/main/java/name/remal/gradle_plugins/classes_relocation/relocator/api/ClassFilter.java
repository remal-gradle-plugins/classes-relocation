package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static java.util.function.Predicate.not;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;

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
import name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils;
import name.remal.gradle_plugins.toolkit.GlobPattern;

@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ClassFilter {

    private final Collection<GlobPattern> inclusions = new ArrayList<>();

    private final Collection<GlobPattern> exclusions = new ArrayList<>();

    public boolean matches(@Nullable String classNameOrInternalName) {
        if (classNameOrInternalName == null) {
            return false;
        }

        var classInternalName = toClassInternalName(classNameOrInternalName);
        return matches(inclusions, classInternalName, true)
            && !matches(exclusions, classInternalName, false);
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


    @CanIgnoreReturnValue
    public ClassFilter include(Iterable<String> includes) {
        StreamSupport.stream(includes.spliterator(), false)
            .filter(Objects::nonNull)
            .map(AsmUtils::toClassInternalName)
            .map(GlobPattern::compile)
            .filter(not(inclusions::contains))
            .forEach(inclusions::add);
        return this;
    }

    @CanIgnoreReturnValue
    public ClassFilter include(String... includes) {
        return include(List.of(includes));
    }

    @CanIgnoreReturnValue
    public ClassFilter exclude(Iterable<String> excludes) {
        StreamSupport.stream(excludes.spliterator(), false)
            .filter(Objects::nonNull)
            .map(AsmUtils::toClassInternalName)
            .map(GlobPattern::compile)
            .filter(not(exclusions::contains))
            .forEach(exclusions::add);
        return this;
    }

    @CanIgnoreReturnValue
    public ClassFilter exclude(String... excludes) {
        return exclude(List.of(excludes));
    }


    @CanIgnoreReturnValue
    public ClassFilter copyFrom(ClassFilter other) {
        other.inclusions.stream()
            .filter(not(inclusions::contains))
            .forEach(inclusions::add);
        other.exclusions.stream()
            .filter(not(exclusions::contains))
            .forEach(exclusions::add);
        return this;
    }


    @CanIgnoreReturnValue
    public ClassFilter negate() {
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
