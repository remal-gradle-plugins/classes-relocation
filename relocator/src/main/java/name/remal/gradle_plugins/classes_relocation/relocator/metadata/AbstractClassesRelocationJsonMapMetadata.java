package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.parseJsonObject;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.writeJsonObjectToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;

public abstract class AbstractClassesRelocationJsonMapMetadata<Type>
    extends AbstractClassesRelocationMetadata<Type, Map<String, ?>> {

    @Nullable
    @Override
    protected Map<String, ?> deserializeStorage(Resource resource) {
        return parseJsonObject(resource);
    }

    @Override
    protected Map<String, Object> mergeStorages(List<Map<String, ?>> maps) {
        val result = new LinkedHashMap<String, Object>();
        maps.forEach(map -> map.forEach(result::putIfAbsent));
        return result;
    }

    @Override
    protected byte[] serializeStorage(Map<String, ?> map) {
        val text = writeJsonObjectToString(map);
        return text.getBytes(UTF_8);
    }

}
