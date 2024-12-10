package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static lombok.AccessLevel.PRIVATE;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
class ReachableComparator implements Comparator<Reachable> {

    public static final ReachableComparator REACHABLE_COMPARATOR = new ReachableComparator();


    private static final Map<Class<?>, Integer> TYPE_ORDERS = ImmutableMap.of(
        ReachableClass.class, 0,
        ReachableField.class, 10,
        ReachableMethod.class, 11,
        ReachableResource.class, 1000
    );

    private static final Comparator<ReachableClass> REACHABLE_CLASS_COMPARATOR =
        comparing(ReachableClass::getClassInternalName);

    private static final Comparator<ReachableField> REACHABLE_FIELD_COMPARATOR =
        comparing(ReachableField::getOwnerClassInternalName)
            .thenComparing(ReachableField::getFieldName);

    private static final Comparator<ReachableMethod> REACHABLE_METHOD_COMPARATOR =
        comparing(ReachableMethod::getOwnerClassInternalName)
            .thenComparing(ReachableMethod::getMethodName)
            .thenComparing(ReachableMethod::getMethodDescriptor);

    private static final Comparator<ReachableResource> REACHABLE_RESOURCE_COMPARATOR =
        comparing(ReachableResource::getResourcePath);

    @Override
    public int compare(Reachable o1, Reachable o2) {
        val class1 = o1.getClass();
        val order1 = TYPE_ORDERS.get(class1);
        if (order1 == null) {
            throw new UnsupportedOperationException("Unsupported: " + o1);
        }
        val class2 = o2.getClass();
        val order2 = TYPE_ORDERS.get(class2);
        if (order2 == null) {
            throw new UnsupportedOperationException("Unsupported: " + o2);
        }
        if (class1 != class2 && Objects.equals(order1, order2)) {
            throw new IllegalStateException(format(
                "Different classes can't have the same order %s: %s and %s",
                order1,
                class1,
                class2
            ));
        }

        int result = Integer.compare(order1, order2);
        if (result != 0) {
            return result;
        }

        if (class1 == ReachableClass.class) {
            return REACHABLE_CLASS_COMPARATOR
                .compare((ReachableClass) o1, (ReachableClass) o2);
        }

        if (class1 == ReachableField.class) {
            return REACHABLE_FIELD_COMPARATOR
                .compare((ReachableField) o1, (ReachableField) o2);
        }

        if (class1 == ReachableMethod.class) {
            return REACHABLE_METHOD_COMPARATOR
                .compare((ReachableMethod) o1, (ReachableMethod) o2);
        }

        if (class2 == ReachableResource.class) {
            return REACHABLE_RESOURCE_COMPARATOR
                .compare((ReachableResource) o1, (ReachableResource) o2);
        }

        throw new UnsupportedOperationException("Unsupported: " + o1);
    }

}
