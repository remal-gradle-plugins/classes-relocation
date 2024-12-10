package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ReachableClass implements Reachable {

    public static ReachableClass reachableClassFor(String classInternalName) {
        return new ReachableClass(classInternalName);
    }


    String classInternalName;

}
