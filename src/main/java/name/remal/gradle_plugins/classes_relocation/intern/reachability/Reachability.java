package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.classes_relocation.intern.reachability.ReachableClass.reachableClassFor;
import static name.remal.gradle_plugins.classes_relocation.intern.reachability.ReachableField.reachableFieldFor;
import static name.remal.gradle_plugins.classes_relocation.intern.reachability.ReachableMethod.reachableMethodFor;
import static name.remal.gradle_plugins.classes_relocation.intern.reachability.ReachableResource.reachableResourceFor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.val;

public class Reachability {

    private final Map<String, ReachabilityClassInfo> classesReachability = new LinkedHashMap<>();
    private final Set<String> reachableResources = new LinkedHashSet<>();


    @Nullable
    public ReachableClass registerReachableClass(String classInternalName) {
        if (!classesReachability.containsKey(classInternalName)) {
            classesReachability.put(classInternalName, new ReachabilityClassInfo(classInternalName));
            return reachableClassFor(classInternalName);
        }

        return null;
    }

    public void markInstanceReachable(String classInternalName) {
        val classReachability = requireNonNull(classesReachability.get(classInternalName));
        classReachability.setInstanceReachable(true);
    }

    @Nullable
    public ReachableClass registerParentClass(String classInternalName, String parentClassInternalName) {
        val classReachability = requireNonNull(classesReachability.get(classInternalName));

        val newReachableClass = registerReachableClass(parentClassInternalName);
        val parentClassReachability = requireNonNull(classesReachability.get(parentClassInternalName));

        classReachability.addParent(parentClassReachability);

        return newReachableClass;
    }

    @Nullable
    public ReachableField registerReachableField(String ownerClassInternalName, String fieldName) {
        val classReachability = requireNonNull(classesReachability.get(ownerClassInternalName));
        if (classReachability.addReachableField(fieldName)) {
            return reachableFieldFor(ownerClassInternalName, fieldName);
        }

        return null;
    }

    @Nullable
    public ReachableMethod registerReachableMethod(
        String ownerClassInternalName,
        String methodName,
        String methodDescriptor
    ) {
        val classReachability = requireNonNull(classesReachability.get(ownerClassInternalName));
        if (classReachability.addReachableMethod(methodName, methodDescriptor)) {
            return reachableMethodFor(ownerClassInternalName, methodName, methodDescriptor);
        }

        return null;
    }

    @Nullable
    public ReachableResource registerReachableResource(String resourcePath) {
        if (reachableResources.add(resourcePath)) {
            return reachableResourceFor(resourcePath);
        }

        return null;
    }


    public boolean isClassReachable(String classInternalName) {
        return classesReachability.containsKey(classInternalName);
    }

    public boolean isFieldReachable(String ownerClassInternalName, String fieldName) {
        val classReachability = classesReachability.get(ownerClassInternalName);
        if (classReachability == null) {
            return false;
        }

        return classReachability.isFieldReachable(fieldName);
    }

    public boolean isMethodReachable(
        String ownerClassInternalName,
        String methodName,
        String methodDescriptor,
        int accessModifiers
    ) {
        val classReachability = classesReachability.get(ownerClassInternalName);
        if (classReachability == null) {
            return false;
        }

        return classReachability.isMethodReachable(methodName, methodDescriptor, accessModifiers);
    }

}
