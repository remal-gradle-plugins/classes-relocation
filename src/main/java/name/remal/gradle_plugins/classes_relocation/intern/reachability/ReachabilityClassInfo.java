package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toMethodParamsDescriptor;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import com.google.errorprone.annotations.DoNotCall;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.val;

@RequiredArgsConstructor
@ToString
class ReachabilityClassInfo {

    @Getter
    private final String classInternalName;

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }


    @Setter
    @Getter
    private boolean isInstanceReachable;


    private final Set<ReachabilityClassInfo> parents = new LinkedHashSet<>();
    private final Set<ReachabilityClassInfo> children = new LinkedHashSet<>();

    public void addParent(ReachabilityClassInfo parent) {
        parents.add(parent);
        parent.children.add(this);
    }


    private final Set<String> reachableFieldNames = new LinkedHashSet<>();

    public boolean addReachableField(String fieldName) {
        return reachableFieldNames.add(fieldName);
    }

    public boolean isFieldReachable(String fieldName) {
        return reachableFieldNames.contains(fieldName);
    }


    private final Set<MethodKey> reachableMethods = new LinkedHashSet<>();

    public boolean addReachableMethod(String methodName, String methodDescriptor) {
        val methodKey = getMethodKey(methodName, methodDescriptor);
        return reachableMethods.add(methodKey);
    }

    public boolean isMethodReachable(String methodName, String methodDescriptor, int accessModifiers) {
        val methodKey = getMethodKey(methodName, methodDescriptor);
        if (reachableMethods.contains(methodKey)) {
            return true;
        }

        if ((accessModifiers & ACC_PRIVATE) != 0
            || (accessModifiers & ACC_STATIC) != 0
        ) {
            return false;
        }


        Set<ReachabilityClassInfo> processed = new LinkedHashSet<>();

        Deque<ReachabilityClassInfo> reachabilityQueue = new ArrayDeque<>(parents);
        while (true) {
            val reachability = reachabilityQueue.poll();
            if (reachability == null) {
                break;
            }

            if (reachability.reachableMethods.contains(methodKey)) {
                return true;
            }

            reachability.parents.stream()
                .filter(processed::add)
                .forEach(reachabilityQueue::addLast);
        }

        if ((accessModifiers & ACC_FINAL) != 0) {
            return false;
        }

        reachabilityQueue.clear();
        reachabilityQueue.addAll(children);
        while (true) {
            val reachability = reachabilityQueue.poll();
            if (reachability == null) {
                break;
            }

            if (reachability.reachableMethods.contains(methodKey)) {
                return true;
            }

            reachability.children.stream()
                .filter(processed::add)
                .forEach(reachabilityQueue::addLast);
        }

        return false;
    }


    private static MethodKey getMethodKey(String methodName, String methodDescriptor) {
        val methodParamsDescriptor = toMethodParamsDescriptor(methodDescriptor);
        return new MethodKey(methodName, methodParamsDescriptor);
    }

    @Value
    @EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
    @RequiredArgsConstructor(onConstructor = @__(@DoNotCall))
    private static class MethodKey {
        String methodName;
        String methodParamsDescriptor;
    }

}
