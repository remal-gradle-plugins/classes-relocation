package name.remal.gradle_plugins.classes_relocation.intern.task.queued.manifest;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessManifest implements QueuedTask {

    @Override
    public int getPhase() {
        return AGGREGATE_PHASE;
    }

}
