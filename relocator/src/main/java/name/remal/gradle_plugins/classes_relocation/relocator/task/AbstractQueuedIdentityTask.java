package name.remal.gradle_plugins.classes_relocation.relocator.task;

import static java.lang.System.identityHashCode;

import javax.annotation.Nullable;

public abstract class AbstractQueuedIdentityTask implements QueuedTask {

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

}
