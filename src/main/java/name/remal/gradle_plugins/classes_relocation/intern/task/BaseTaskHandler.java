package name.remal.gradle_plugins.classes_relocation.intern.task;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface BaseTaskHandler extends Comparable<BaseTaskHandler> {

    default int getOrder() {
        return 0;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    default int compareTo(BaseTaskHandler other) {
        return Integer.compare(getOrder(), other.getOrder());
    }

}
