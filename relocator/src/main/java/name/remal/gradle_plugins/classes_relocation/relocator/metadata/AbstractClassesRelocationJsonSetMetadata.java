package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.parseJsonArray;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.writeJsonArrayToString;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import org.jspecify.annotations.Nullable;

public abstract class AbstractClassesRelocationJsonSetMetadata<Element>
    extends AbstractClassesRelocationMetadata<Set<Element>, Set<Object>> {

    @Nullable
    protected abstract Element readStorageElement(Object element);

    @Nullable
    protected abstract Object createStorageElement(Element element);


    @Override
    protected Set<Element> newInstance() {
        return new LinkedHashSet<>();
    }


    @Nullable
    @Override
    protected Set<Object> deserializeStorage(Resource resource) {
        var jsonArray = parseJsonArray(resource);
        return jsonArray != null ? new LinkedHashSet<>(jsonArray) : null;
    }

    @Override
    protected Set<Object> mergeStorages(List<Set<Object>> lists) {
        return lists.stream()
            .flatMap(Collection::stream)
            .collect(toCollection(LinkedHashSet::new));
    }

    @Override
    protected void fillFromStorage(Set<Object> set) {
        set.stream()
            .map(this::readStorageElement)
            .filter(Objects::nonNull)
            .forEach(getCurrent()::add);
    }

    @Override
    protected boolean isNextEmpty() {
        return getNext().isEmpty();
    }

    @Override
    protected Set<Object> createAndPopulateStorage() {
        return getNext().stream()
            .filter(Objects::nonNull)
            .map(this::createStorageElement)
            .filter(Objects::nonNull)
            .collect(toCollection(LinkedHashSet::new));
    }

    @Override
    protected byte[] serializeStorage(Set<Object> list) {
        var text = writeJsonArrayToString(list);
        return text.getBytes(UTF_8);
    }

}
