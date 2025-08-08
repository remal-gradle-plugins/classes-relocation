@Value.Style(
    defaults = @Value.Immutable(
        prehash = true
    ),
    stagedBuilder = true,
    jacksonIntegration = false,
    alwaysPublicInitializers = false,
    visibility = ImplementationVisibility.SAME,
    builderVisibility = BuilderVisibility.PUBLIC,
    get = {"is*", "get*"},
    optionalAcceptNullable = true,
    typeBuilder = "*Builder",
    typeInnerBuilder = "BaseBuilder",
    allowedClasspathAnnotations = {
        org.immutables.value.Generated.class,
        Nullable.class,
        Immutable.class,
        ThreadSafe.class,
        NotThreadSafe.class,
    },
    depluralize = true
)
@NullMarked
package name.remal.gradle_plugins.classes_relocation.relocator.task;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.jspecify.annotations.NullMarked;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;
