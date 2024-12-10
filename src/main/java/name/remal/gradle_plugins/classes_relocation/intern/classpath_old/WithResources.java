package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.withoutMultiReleasePathPrefix;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.val;
import name.remal.gradle_plugins.toolkit.GlobPattern;
import org.jetbrains.annotations.Unmodifiable;

public interface WithResources extends Closeable {

    @Unmodifiable
    List<Resource> getResources();


    @Unmodifiable
    default List<Resource> getResources(String... inclusions) {
        return getResources(
            /* inclusions = */ ImmutableList.copyOf(inclusions),
            /* exclusions = */ ImmutableList.of()
        );
    }

    @Unmodifiable
    @SuppressWarnings("java:S3776")
    default List<Resource> getResources(Collection<String> inclusions, Collection<String> exclusions) {
        if (inclusions.isEmpty() && exclusions.isEmpty()) {
            return getResources();
        }

        val inclusionPatterns = inclusions.stream()
            .map(GlobPattern::compile)
            .distinct()
            .collect(toList());

        val exclusionPatterns = exclusions.stream()
            .map(GlobPattern::compile)
            .distinct()
            .collect(toList());

        return getResources().stream()
            .filter(resource -> {
                if (inclusionPatterns.isEmpty()) {
                    return true;
                }

                val path = resource.getPath();
                for (val pattern : inclusionPatterns) {
                    if (pattern.matches(path)) {
                        return true;
                    }
                }
                return false;
            })
            .filter(resource -> {
                if (exclusionPatterns.isEmpty()) {
                    return true;
                }

                val path = resource.getPath();
                for (val pattern : exclusionPatterns) {
                    if (pattern.matches(path)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(toImmutableList());
    }


    @Unmodifiable
    default Set<String> getResourcePaths() {
        return getResources().stream()
            .map(Resource::getPath)
            .sorted()
            .collect(toImmutableSet());
    }


    default boolean hasResource(Resource resource) {
        return getResources().contains(resource);
    }

    default boolean hasResource(String resourcePath) {
        return getResourcePaths().contains(resourcePath);
    }


    @Unmodifiable
    default Map<String, List<Resource>> getClasses() {
        val map = new LinkedHashMap<String, Map<String, Resource>>();
        val classResources = getResources(
            /* inclusions = */ ImmutableList.of("**/*.class"),
            /* exclusions = */ ImmutableList.of("**/module-info.class")
        );
        classResources.forEach(resource -> {
            val normalizedPath = withoutMultiReleasePathPrefix(resource.getPath());
            val className = normalizedPath.substring(0, normalizedPath.length() - ".class".length())
                .replace('/', '.');
            val allClassResources = map.computeIfAbsent(className, __ -> new LinkedHashMap<>());
            allClassResources.putIfAbsent(resource.getPath(), resource);
        });

        val immutableBuilder = ImmutableMap.<String, List<Resource>>builder();
        map.forEach((className, resources) ->
            immutableBuilder.put(className, ImmutableList.copyOf(resources.values()))
        );
        return immutableBuilder.build();
    }

    @Unmodifiable
    default List<Resource> getClassResources(String classNameOrInternalName) {
        val className = toClassName(classNameOrInternalName);
        return defaultValue(getClasses().get(className));
    }

    default boolean hasClass(String classNameOrInternalName) {
        return !getClassResources(classNameOrInternalName).isEmpty();
    }

    @Unmodifiable
    default Set<String> getPackageNames() {
        return getClasses().keySet().stream()
            .map(className -> substringBeforeLast(className, ".", ""))
            .collect(toImmutableSet());
    }

}
