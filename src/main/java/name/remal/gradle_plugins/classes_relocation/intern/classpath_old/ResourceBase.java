package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.classes_relocation.intern.content.Content.contentForInputStreamSupplier;

import javax.annotation.Nullable;
import lombok.Getter;
import name.remal.gradle_plugins.classes_relocation.intern.content.Content;

@Getter
abstract class ResourceBase implements Resource {

    private final Content content = contentForInputStreamSupplier(this::open);


    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

}
