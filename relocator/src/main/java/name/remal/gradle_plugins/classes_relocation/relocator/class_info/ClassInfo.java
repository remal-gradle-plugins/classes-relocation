package name.remal.gradle_plugins.classes_relocation.relocator.class_info;

import static java.lang.System.identityHashCode;
import static java.util.Collections.unmodifiableSet;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

@Builder
@RequiredArgsConstructor(access = PRIVATE)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(makeFinal = true, level = PRIVATE)
public class ClassInfo {

    @ToString.Include
    String internalClassName;

    @Default
    @ToString.Include
    @Getter(PRIVATE)
    boolean resolved = true;

    @Unmodifiable
    @Singular
    Set<String> fields;

    @Unmodifiable
    @Singular
    Set<String> accessibleFields;

    @Unmodifiable
    @Singular
    Set<MethodKey> constructors;

    @Unmodifiable
    @Singular
    Set<MethodKey> accessibleConstructors;

    @Unmodifiable
    @Singular
    Set<MethodKey> methods;

    @Unmodifiable
    @Singular
    Set<MethodKey> accessibleMethods;

    @Unmodifiable
    @Singular
    Set<MethodKey> overrideableMethods;

    @Unmodifiable
    @Singular
    Set<String> permittedSubclassInternalNames;

    @Unmodifiable
    @Singular
    Set<ClassInfo> parentClasses;

    @Getter(NONE)
    Set<ClassInfo> childClasses = new LinkedHashSet<>();

    public void addChildClass(ClassInfo childClass) {
        childClasses.add(childClass);
    }

    @UnmodifiableView
    public Set<ClassInfo> getChildClasses() {
        return unmodifiableSet(childClasses);
    }


    @Unmodifiable
    Set<ClassInfo> allParentClasses = asLazySetProxy(() ->
        getAllClasses(ClassInfo::getParentClasses)
    );

    @Unmodifiable
    public Set<ClassInfo> getAllChildClasses() {
        return getAllClasses(ClassInfo::getChildClasses);
    }

    @Unmodifiable
    private Set<ClassInfo> getAllClasses(Function<ClassInfo, Set<ClassInfo>> getter) {
        val result = new LinkedHashSet<ClassInfo>();
        val queue = new ArrayDeque<>(getter.apply(this));
        while (true) {
            val parentClass = queue.poll();
            if (parentClass == null) {
                break;
            }

            if (result.add(parentClass)) {
                queue.addAll(getter.apply(parentClass));
            }
        }

        return ImmutableSet.copyOf(result);
    }


    @Getter(NONE)
    private final LazyValue<Boolean> areAllResolved = lazyValue(() ->
        isResolved()
            && getParentClasses().stream().allMatch(ClassInfo::isResolved)
    );

    public boolean areAllResolved() {
        return areAllResolved.get();
    }


    public boolean hasField(String fieldName) {
        return fields.contains(fieldName);
    }

    public boolean hasAccessibleField(String fieldName) {
        return accessibleFields.contains(fieldName);
    }

    public boolean hasMethod(MethodKey methodKey) {
        return methods.contains(methodKey);
    }

    public boolean hasAccessibleMethod(MethodKey methodKey) {
        return accessibleMethods.contains(methodKey);
    }

    public boolean hasOverrideableMethod(MethodKey methodKey) {
        return overrideableMethods.contains(methodKey);
    }


    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

}
