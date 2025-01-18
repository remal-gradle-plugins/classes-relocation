package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public abstract class AbstractClassesRelocationJsonDictionaryMetadata
    extends AbstractClassesRelocationJsonMapMetadata<Map<String, String>> {

    @Override
    protected Map<String, String> newInstance() {
        return isDebugEnabled() ? new TreeMap<>() : new LinkedHashMap<>();
    }

    @Override
    protected void fillFromStorage(Map<String, ?> map) {
        map.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }

            getCurrent().put(key, value.toString());
        });
    }

    @Override
    protected boolean isNextEmpty() {
        return getNext().isEmpty();
    }

    @Override
    protected Map<String, ?> createAndPopulateStorage() {
        return new TreeMap<>(getNext());
    }

}
