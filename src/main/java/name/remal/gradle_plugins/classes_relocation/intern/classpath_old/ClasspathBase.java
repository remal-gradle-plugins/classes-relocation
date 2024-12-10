package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static java.lang.System.identityHashCode;

import javax.annotation.Nullable;

abstract class ClasspathBase extends WithResourcesBase implements Classpath {

    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

}
