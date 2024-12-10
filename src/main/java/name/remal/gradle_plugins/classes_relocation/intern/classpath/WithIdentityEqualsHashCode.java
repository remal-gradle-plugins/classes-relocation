package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.lang.System.identityHashCode;

import javax.annotation.Nullable;

abstract class WithIdentityEqualsHashCode {

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

}
