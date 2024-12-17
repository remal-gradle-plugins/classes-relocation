package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

public interface QueuedTask extends Comparable<QueuedTask> {

    int PROCESS_SOURCES_PHASE = doNotInline(0);
    int RELOCATE_PHASE = PROCESS_SOURCES_PHASE + 100;
    int AGGREGATE_PHASE = RELOCATE_PHASE + 100;

    int getPhase();

    @Override
    @OverridingMethodsMustInvokeSuper
    default int compareTo(QueuedTask other) {
        int result = Integer.compare(getPhase(), other.getPhase());
        if (result != 0) {
            return result;
        }

        if (getClass() != other.getClass()) {
            result = getClass().getName().compareTo(other.getClass().getName());
            if (result != 0) {
                return result;
            }

            result = Integer.compare(identityHashCode(getClass()), identityHashCode(other.getClass()));
        }

        return result;
    }

}
