package name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import lombok.With;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

@Value
@With
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class CopySourceResource implements QueuedTask {

    String resourceName;

    @Override
    public int getPhase() {
        return PROCESS_REMAINING_SOURCES_PHASE;
    }

}
