package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ReachableMethod implements Reachable {

    public static ReachableMethod reachableMethodFor(
        String ownerClassInternalName,
        String methodName,
        String methodDescriptor
    ) {
        return new ReachableMethod(ownerClassInternalName, methodName, methodDescriptor);
    }


    String ownerClassInternalName;
    String methodName;
    String methodDescriptor;

}
