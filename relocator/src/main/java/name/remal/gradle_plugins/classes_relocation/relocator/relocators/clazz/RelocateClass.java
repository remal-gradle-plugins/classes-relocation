package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateClass implements QueuedTask {

    String classInternalName;


    @Override
    public int getPhase() {
        return RELOCATE_PHASE;
    }

}
