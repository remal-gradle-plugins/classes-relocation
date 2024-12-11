package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PACKAGE;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.ToString;

@Getter(PACKAGE)
@ToString
public class ResourcesFilter {

    private final Set<String> inclusions = new LinkedHashSet<>();

    private final Set<String> exclusions = new LinkedHashSet<>();

    ResourcesFilter() {
    }

    ResourcesFilter(ResourcesFilter other) {
        inclusions.addAll(other.inclusions);
        exclusions.addAll(other.exclusions);
    }

    boolean isEmpty() {
        return inclusions.isEmpty() && exclusions.isEmpty();
    }


    public void includePackages(Iterable<String> packageNames) {
        addPackages(inclusions, packageNames);
    }

    public void includePackage(String... packageNames) {
        includePackages(asList(packageNames));
    }

    public void excludePackages(Iterable<String> packageNames) {
        addPackages(exclusions, packageNames);
    }

    public void excludePackage(String... packageNames) {
        excludePackages(asList(packageNames));
    }

    private static void addPackages(Set<String> patterns, Iterable<String> packageNames) {
        StreamSupport.stream(packageNames.spliterator(), false)
            .filter(Objects::nonNull)
            .map(name -> toClassInternalName(name) + "/*")
            .forEach(patterns::add);
    }

}
