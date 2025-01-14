package name.remal.gradle_plugins.classes_relocation.relocator.relocators.license;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class CopyRelocationLicenses implements QueuedTask {

    @Override
    public int getPhase() {
        return AGGREGATE_PHASE;
    }

}
