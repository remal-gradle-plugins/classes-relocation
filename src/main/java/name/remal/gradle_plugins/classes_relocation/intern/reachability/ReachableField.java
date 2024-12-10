package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ReachableField implements Reachable {

    public static ReachableField reachableFieldFor(String ownerClassInternalName, String fieldName) {
        return new ReachableField(ownerClassInternalName, fieldName);
    }


    String ownerClassInternalName;
    String fieldName;

}
