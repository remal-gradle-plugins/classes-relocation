package name.remal.gradle_plugins.classes_relocation.intern.resource_handler;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ResourceHandler {

    Collection<String> getInclusions();

    default Collection<String> getExclusions() {
        return ImmutableList.of();
    }

    default int getPriority() {
        return 0;
    }

}
