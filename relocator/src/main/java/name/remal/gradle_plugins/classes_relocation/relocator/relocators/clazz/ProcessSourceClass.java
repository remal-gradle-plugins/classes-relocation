package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessSourceClass implements QueuedTask {

    String sourceClassName;


    @Override
    public int getPhase() {
        return PROCESS_SOURCES_PHASE;
    }

}
