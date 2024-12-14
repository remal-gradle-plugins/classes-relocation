package name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateClass implements QueuedTask {

    String classInternalName;


    @Override
    public int getPhase() {
        return RELOCATE_PHASE;
    }

}
