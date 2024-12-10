package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class ReachableResource implements Reachable {

    public static ReachableResource reachableResourceFor(String resourcePath) {
        return new ReachableResource(resourcePath);
    }


    String resourcePath;

}
