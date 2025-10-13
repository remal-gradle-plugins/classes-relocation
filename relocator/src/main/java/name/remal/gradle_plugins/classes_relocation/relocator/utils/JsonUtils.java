package name.remal.gradle_plugins.classes_relocation.relocator.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import org.gradle.api.logging.LogLevel;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;
import org.jspecify.annotations.Nullable;

@CustomLog
@NoArgsConstructor(access = PRIVATE)
public abstract class JsonUtils {

    private static final int JSON_INDENT = 2;

    private static final boolean IN_TEST = isInTest();

    private static final JSONParserConfiguration JSON_PARSER_CONFIGURATION = new JSONParserConfiguration()
        .withStrictMode(false)
        .withOverwriteDuplicateKey(true);


    @Language("JSON")
    public static String writeJsonObjectToString(Map<String, ?> map) {
        var jsonObject = new JSONObject(map);
        return jsonObject.toString(JSON_INDENT);
    }

    @Language("JSON")
    public static String writeJsonArrayToString(Collection<?> collection) {
        var jsonObject = new JSONArray(collection);
        return jsonObject.toString(JSON_INDENT);
    }


    @Nullable
    @Contract("null->null")
    @SneakyThrows
    public static Map<String, Object> parseJsonObject(@Nullable Resource resource) {
        if (resource == null) {
            return null;
        }

        var content = resource.readText(UTF_8);
        try {
            var jsonObject = new JSONObject(content, JSON_PARSER_CONFIGURATION);
            removeJsonNullValues(jsonObject);
            return jsonObject.toMap();

        } catch (JSONException e) {
            logger.log(
                IN_TEST ? LogLevel.WARN : LogLevel.DEBUG,
                "Error parsing JSONObject from" + resource,
                e
            );
            return null;
        }
    }

    @Nullable
    @Contract("null->null")
    @SneakyThrows
    public static List<Object> parseJsonArray(@Nullable Resource resource) {
        if (resource == null) {
            return null;
        }

        var content = resource.readText(UTF_8);
        try {
            var jsonArray = new JSONArray(content, JSON_PARSER_CONFIGURATION);
            removeJsonNullValues(jsonArray);
            return jsonArray.toList();

        } catch (JSONException e) {
            logger.log(
                IN_TEST ? LogLevel.WARN : LogLevel.DEBUG,
                "Error parsing JSONArray from" + resource,
                e
            );
            return null;
        }
    }

    private static void removeJsonNullValues(@Nullable Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof JSONObject) {
            removeJsonNullValues((JSONObject) value);
        } else if (value instanceof JSONArray) {
            removeJsonNullValues((JSONArray) value);
        }
    }

    private static void removeJsonNullValues(JSONObject jsonObject) {
        for (var key : new ArrayList<>(jsonObject.keySet())) {
            var value = jsonObject.opt(key);
            if (isNullJsonValue(value)) {
                jsonObject.remove(key);
                continue;
            }

            removeJsonNullValues(value);
        }
    }

    @SuppressWarnings("java:S127")
    private static void removeJsonNullValues(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            var value = jsonArray.opt(i);
            if (isNullJsonValue(value)) {
                jsonArray.remove(i);
                --i;
                continue;
            }

            removeJsonNullValues(value);
        }
    }

    @Contract("null->true")
    private static boolean isNullJsonValue(@Nullable Object value) {
        return value == null || value == JSONObject.NULL;
    }

}
