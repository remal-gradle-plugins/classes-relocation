package name.remal.gradle_plugins.classes_relocation.intern.task.queued.meta_inf_services;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateMetaInfServices implements QueuedTask {

    String serviceClassInternalName;


    @Override
    public int getPhase() {
        return RELOCATE_PHASE;
    }

}
