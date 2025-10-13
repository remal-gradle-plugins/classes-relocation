package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.lang.System.identityHashCode;

import org.jspecify.annotations.Nullable;

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
