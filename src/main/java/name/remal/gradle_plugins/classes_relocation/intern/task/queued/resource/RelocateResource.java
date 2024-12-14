package name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateResource implements QueuedTask {

    String resourceName;

    String updatedResourceName;


    @Override
    public int getPhase() {
        return RELOCATE_PHASE;
    }

}
