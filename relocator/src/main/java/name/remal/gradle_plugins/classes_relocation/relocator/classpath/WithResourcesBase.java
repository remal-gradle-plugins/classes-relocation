package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.ResourceKey.resourceKeyFor;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyMapProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;
import org.jetbrains.annotations.Unmodifiable;

@Getter
@CustomLog
abstract class WithResourcesBase extends WithIdentityEqualsHashCode implements WithResources {

    @Getter(AccessLevel.NONE)
    protected final ClosablesContainer closables = new ClosablesContainer();

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        closables.close();
    }


    @Unmodifiable
    private final List<Resource> allResources = asLazyListProxy(() ->
        readResources().stream().collect(toImmutableList())
    );

    protected abstract Collection<Resource> readResources() throws Exception;


    @Unmodifiable
    private final Map<String, @Unmodifiable List<Resource>> resources = asLazyMapProxy(() -> {
        val map = getAllResources().stream()
            .collect(groupingBy(Resource::getName, LinkedHashMap::new, toImmutableList()));
        return ImmutableMap.copyOf(map);
    });


    private final Map<String, List<Resource>> internalClassNameToResources = asLazyMapProxy(() -> {
        val builder = ImmutableMap.<String, List<Resource>>builder();
        val processedResourceKeys = new LinkedHashSet<ResourceKey>();
        for (val internalClassName : getClassInternalNames()) {
            val classResources = getResources(internalClassName + ".class").stream()
                .filter(resource -> processedResourceKeys.add(resourceKeyFor(resource)))
                .collect(toImmutableList());
            builder.put(internalClassName, classResources);
        }
        return builder.build();
    });

    @Override
    @Unmodifiable
    public List<Resource> getClassResources(String classNameOrInternalName) {
        val internalClassName = toClassInternalName(classNameOrInternalName);
        return defaultValue(internalClassNameToResources.get(internalClassName), emptyList());
    }


    @Unmodifiable
    private final Set<String> classInternalNames = asLazySetProxy(() ->
        getResources().keySet().stream()
            .filter(name -> name.endsWith(".class"))
            .map(name -> name.substring(0, name.length() - ".class".length()))
            .filter(name -> !name.equals("module-info"))
            .sorted()
            .collect(toImmutableSet())
    );

    @Unmodifiable
    private final Set<String> classNames = asLazySetProxy(() ->
        getClassInternalNames().stream()
            .map(AsmUtils::toClassName)
            .collect(toImmutableSet())
    );

    @Unmodifiable
    private final Set<String> packageNames = asLazySetProxy(() ->
        getClassNames().stream()
            .map(className -> substringBeforeLast(className, ".", ""))
            .collect(toImmutableSet())
    );

}
