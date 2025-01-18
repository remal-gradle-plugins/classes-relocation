package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ComparatorUtils.compareClasses;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

public interface QueuedTask extends Comparable<QueuedTask> {

    int PROCESS_SOURCES_PHASE = doNotInline(0);
    int RELOCATE_PHASE = PROCESS_SOURCES_PHASE + 100;
    int AGGREGATE_PHASE = RELOCATE_PHASE + 100;
    int PROCESS_REMAINING_SOURCES_PHASE = AGGREGATE_PHASE + 100;

    int getPhase();

    default void onHandled() {
        // do nothing
    }

    default void onNotHandled() {
        throw new NotHandledTaskException(this);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    default int compareTo(QueuedTask other) {
        int result = Integer.compare(getPhase(), other.getPhase());
        if (result != 0) {
            return result;
        }

        return compareClasses(this.getClass(), other.getClass());
    }

}
