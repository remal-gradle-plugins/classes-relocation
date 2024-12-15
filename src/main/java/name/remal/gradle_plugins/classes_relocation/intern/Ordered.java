package name.remal.gradle_plugins.classes_relocation.intern;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface Ordered extends Comparable<Ordered> {

    default int getOrder() {
        return 0;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    default int compareTo(Ordered other) {
        return Integer.compare(getOrder(), other.getOrder());
    }

}
