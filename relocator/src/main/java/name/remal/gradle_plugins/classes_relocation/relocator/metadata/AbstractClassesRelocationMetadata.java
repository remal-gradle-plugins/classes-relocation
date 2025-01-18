package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorLifecycleComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Contract;

@Getter
public abstract class AbstractClassesRelocationMetadata<Type, Storage>
    implements ClassesRelocatorLifecycleComponent {

    private final Type current = newInstance();

    private final Type next = newInstance();


    @ForOverride
    protected abstract Type newInstance();


    @ForOverride
    protected abstract String getResourceName();


    @ForOverride
    @Nullable
    protected abstract Storage deserializeStorage(Resource resource);

    @ForOverride
    protected abstract Storage mergeStorages(List<Storage> storages);

    @ForOverride
    protected abstract void fillFromStorage(Storage storage);


    @ForOverride
    protected abstract boolean isNextEmpty();

    @ForOverride
    protected abstract Storage createAndPopulateStorage();

    @ForOverride
    protected abstract byte[] serializeStorage(Storage storage);


    @Override
    @OverridingMethodsMustInvokeSuper
    public void prepareRelocation(RelocationContext context) {
        var resources = context.getRelocationClasspath().getResources(getResourceName());
        resources.forEach(context::markResourceAsProcessed);

        var storages = resources.stream()
            .map(this::deserializeStorage)
            .filter(Objects::nonNull)
            .collect(toList());
        if (storages.isEmpty()) {
            return;
        }

        var storage = storages.size() == 1
            ? storages.get(0)
            : mergeStorages(storages);

        fillFromStorage(storage);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void finalizeRelocation(RelocationContext context) {
        if (isNextEmpty()) {
            return;
        }

        var storage = createAndPopulateStorage();
        var bytes = serializeStorage(storage);
        var resource = newGeneratedResource(builder -> builder
            .withName(getResourceName())
            .withoutMultiReleaseVersion()
            .withContent(bytes)
        );
        context.writeToOutput(resource);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 1_000;
    }


    //#region utils

    protected static final boolean IN_TEST = isInTest();

    @Getter(AccessLevel.NONE)
    @SuppressWarnings("Slf4jLoggerShouldBePrivate")
    protected final Logger logger = Logging.getLogger(getClass());


    @Nullable
    @Contract("null,_,_,_->null")
    protected final <T> T castOrWarn(@Nullable Object object, Class<T> type, String description, Resource resource) {
        if (object == null) {
            return null;
        }

        if (type.isInstance(object)) {
            return type.cast(object);
        }

        logger.log(
            IN_TEST ? LogLevel.WARN : LogLevel.DEBUG,
            "{}: not an instance of {}: {}",
            resource,
            type,
            description
        );
        return null;
    }

    @Nullable
    @Contract("null,_,_->null")
    protected final <T> T castOrWarn(@Nullable Object object, Class<T> type, Resource resource) {
        return castOrWarn(object, type, String.valueOf(object), resource);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static <E> Class<Collection<E>> collectionClass() {
        return (Class) Collection.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static <K, V> Class<Map<K, V>> mapClass() {
        return (Class) Map.class;
    }

    //#endregion

}
