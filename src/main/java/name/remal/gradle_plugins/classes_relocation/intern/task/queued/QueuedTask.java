package name.remal.gradle_plugins.classes_relocation.intern.task.queued;

import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

public interface QueuedTask extends Comparable<QueuedTask> {

    int PROCESS_SOURCE_RESOURCES_PHASE = doNotInline(0);
    int RELOCATE_PHASE = PROCESS_SOURCE_RESOURCES_PHASE + 100;

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