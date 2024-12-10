package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static name.remal.gradle_plugins.toolkit.NumbersAwareStringComparator.numbersAwareStringComparator;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils;
import name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils;
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
    private final Collection<Resource> resources = asLazyListProxy(() ->
        readResources().stream()
            .sorted(RESOURCE_COMPARATOR)
            .collect(toImmutableList())
    );

    private static final Comparator<Resource> RESOURCE_COMPARATOR = numbersAwareStringComparator(Resource::getName);

    protected abstract Collection<Resource> readResources() throws Exception;


    @Unmodifiable
    private final Set<String> classNames = asLazySetProxy(() ->
        getResources().stream()
            .map(Resource::getName)
            .filter(name -> name.endsWith(".class"))
            .map(name -> name.substring(0, name.length() - ".class".length()))
            .map(MultiReleaseUtils::withoutMultiReleasePathPrefix)
            .filter(name -> !name.equals("module-info"))
            .map(AsmUtils::toClassName)
            .sorted()
            .collect(toImmutableSet())
    );

    @Unmodifiable
    private final Set<String> packageNames = asLazySetProxy(() ->
        getClassNames().stream()
            .map(className -> substringBeforeLast(className, ".", ""))
            .collect(toImmutableSet())
    );

}
