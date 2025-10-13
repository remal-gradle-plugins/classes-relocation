package name.remal.gradle_plugins.classes_relocation.relocator.minimization;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassName;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.BOOLEAN;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.OBJECT;
import static org.objectweb.asm.Type.SHORT;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getArgumentTypes;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils;
import name.remal.gradle_plugins.toolkit.ObjectUtils;
import org.gradle.api.logging.LogLevel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;

@ApiStatus.Internal
@CustomLog
@NoArgsConstructor(access = PRIVATE)
public abstract class ClassReachabilityConfigUtils {

    private static final boolean IN_TEST = isInTest();


    @Unmodifiable
    @SuppressWarnings("java:S3776")
    public static List<ClassReachabilityConfig> groupClassReachabilityConfigs(
        Collection<ClassReachabilityConfig> classReachabilityConfigs
    ) {
        return classReachabilityConfigs.stream()
            .filter(Objects::nonNull)
            .collect(groupingBy(config -> new ClassReachabilityConfigKey(
                config.getClassInternalName(),
                config.getOnReachedClassInternalName()
            )))
            .values()
            .stream()
            .map(configs -> {
                if (configs.size() == 1) {
                    return configs.get(0);
                }

                var builder = configs.get(0).toBuilder();
                for (int i = 1; i < configs.size(); i++) {
                    var config = configs.get(i);
                    builder.fields(config.getFields());
                    builder.methodsKeys(config.getMethodsKeys());
                    if (config.isAllDeclaredConstructors()) {
                        builder.allDeclaredConstructors(true);
                    }
                    if (config.isAllPublicConstructors()) {
                        builder.allPublicConstructors(true);
                    }
                    if (config.isAllDeclaredMethods()) {
                        builder.allDeclaredMethods(true);
                    }
                    if (config.isAllPublicMethods()) {
                        builder.allPublicMethods(true);
                    }
                    if (config.isAllDeclaredFields()) {
                        builder.allDeclaredFields(true);
                    }
                    if (config.isAllPublicFields()) {
                        builder.allPublicFields(true);
                    }
                    if (config.isAllPermittedSubclasses()) {
                        builder.allPermittedSubclasses(true);
                    }
                }
                return builder.build();
            })
            .collect(toUnmodifiableList());
    }

    @Value
    private static class ClassReachabilityConfigKey {
        String classInternalName;

        @Nullable
        String onReachedClassInternalName;
    }


    @SuppressWarnings("java:S2259")
    public static Map<String, Object> convertClassReachabilityConfigToMap(ClassReachabilityConfig config) {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", toClassName(config.getClassInternalName()));
        Optional.ofNullable(config.getOnReachedClassInternalName())
            .filter(not(String::isEmpty))
            .ifPresent(classInternalName ->
                map.put("condition", ImmutableMap.of("typeReached", toClassName(classInternalName)))
            );
        if (!config.getFields().isEmpty()) {
            var array = new ArrayList<>();
            for (var name : config.getFields()) {
                array.add(ImmutableMap.of("name", name));
            }
            map.put("fields", array);
        }
        if (!config.getMethodsKeys().isEmpty()) {
            var array = new ArrayList<>();
            for (var method : config.getMethodsKeys()) {
                array.add(ImmutableMap.of(
                    "name", method.getName(),
                    "parameterTypes", methodKeyToParamTypes(method)
                ));
            }
            map.put("methods", array);
        }
        if (config.isAllDeclaredConstructors()) {
            map.put("allDeclaredConstructors", true);
        }
        if (config.isAllPublicConstructors()) {
            map.put("allPublicConstructors", true);
        }
        if (config.isAllDeclaredMethods()) {
            map.put("allDeclaredMethods", true);
        }
        if (config.isAllPublicMethods()) {
            map.put("allPublicMethods", true);
        }
        if (config.isAllDeclaredFields()) {
            map.put("allDeclaredFields", true);
        }
        if (config.isAllPublicFields()) {
            map.put("allPublicFields", true);
        }
        if (config.isAllPermittedSubclasses()) {
            map.put("allPermittedSubclasses", true);
        }
        return map;
    }

    /**
     * See
     * <a href="https://www.graalvm.org/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.0.0.json">reachability-metadata-schema-v1.0.0.json</a>
     * and
     * <a href="https://www.graalvm.org/docs/reference-manual/native-image/assets/reflect-config-schema-v1.0.0.json">reflect-config-schema-v1.0.0.json</a>
     */
    @Nullable
    @Contract("null->null")
    @SuppressWarnings({"unchecked", "Slf4jFormatShouldBeConst"})
    public static ClassReachabilityConfig convertMapToClassReachabilityConfig(@Nullable Map<?, ?> map) {
        if (map == null) {
            return null;
        }

        map = new JSONObject(map).toMap(); // deep clone

        // unsupported:
        remove(map, "allDeclaredClasses", "queryAllDeclaredClasses");
        remove(map, "allPublicClasses", "queryAllPublicClasses");
        remove(map, "allRecordComponents", "queryAllRecordComponents");
        remove(map, "allNestMembers", "queryAllNestMembers");
        remove(map, "allSigners", "queryAllSigners");
        remove(map, "unsafeAllocated");

        var classInternalName = Optional.ofNullable(remove(map, "type", "name"))
            .map(Object::toString)
            .filter(ObjectUtils::isNotEmpty)
            .map(ClassReachabilityConfigUtils::paramTypeToType)
            .filter(type -> type.getSort() == OBJECT
                || type.getSort() == ARRAY
            )
            .map(type -> type.getSort() == ARRAY ? type.getElementType() : type)
            .map(Type::getInternalName)
            .orElse(null);
        if (classInternalName == null) {
            return null;
        }

        var builder = ClassReachabilityConfig.builder();

        builder.classInternalName(classInternalName);

        Optional.ofNullable(remove(map, "condition"))
            .filter(Map.class::isInstance)
            .map(obj -> (Map<Object, Object>) obj)
            .map(inner -> remove(inner, "typeReached", "typeReachable"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(ObjectUtils::isNotEmpty)
            .map(AsmUtils::toClassInternalName)
            .ifPresent(builder::onReachedClassInternalName);

        Stream.of(map.remove("fields"), map.remove("queriedFields"))
            .filter(Collection.class::isInstance)
            .map(obj -> (Collection<Object>) obj)
            .flatMap(Collection::stream)
            .filter(Map.class::isInstance)
            .map(obj -> (Map<Object, Object>) obj)
            .map(inner -> remove(inner, "name"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(AsmUtils::toClassInternalName)
            .forEach(builder::field);

        Stream.of(map.remove("methods"), map.remove("queriedMethods"))
            .filter(Collection.class::isInstance)
            .map(obj -> (Collection<Object>) obj)
            .flatMap(Collection::stream)
            .filter(Map.class::isInstance)
            .map(obj -> (Map<Object, Object>) obj)
            .map(inner -> parseMethodKey(inner, classInternalName))
            .filter(Objects::nonNull)
            .forEach(builder::methodsKey);

        builder.allDeclaredConstructors(removeBool(map, "allDeclaredConstructors", "queryAllDeclaredConstructors"));

        builder.allPublicConstructors(removeBool(map, "allPublicConstructors", "queryAllPublicConstructors"));

        builder.allDeclaredMethods(removeBool(map, "allDeclaredMethods", "queryAllDeclaredMethods"));

        builder.allPublicMethods(removeBool(map, "allPublicMethods", "queryAllPublicMethods"));

        builder.allDeclaredFields(removeBool(map, "allDeclaredFields", "queryAllDeclaredFields"));

        builder.allPublicFields(removeBool(map, "allPublicFields", "queryAllPublicFields"));

        builder.allPermittedSubclasses(removeBool(map, "allPermittedSubclasses", "queryAllPermittedSubclasses"));

        if (!map.isEmpty()) {
            var message = format(
                "Unsupported fields of %s class reachability metadata: %s",
                classInternalName,
                map.keySet().stream()
                    .map(String::valueOf)
                    .collect(joining("`, `", "`", "`"))
            );
            if (IN_TEST) {
                throw new AssertionError(message);
            } else {
                logger.debug(message);
            }
        }

        return builder.build();
    }

    private static List<String> methodKeyToParamTypes(MethodKey methodKey) {
        var args = getArgumentTypes(methodKey.getParamsDescriptor() + "V");
        return stream(args)
            .map(ClassReachabilityConfigUtils::typeToParamType)
            .collect(toUnmodifiableList());
    }

    private static String typeToParamType(Type type) {
        if (type.getSort() == OBJECT) {
            return toClassName(type.getInternalName());
        } else if (type.getSort() == BOOLEAN) {
            return "boolean";
        } else if (type.getSort() == CHAR) {
            return "char";
        } else if (type.getSort() == BYTE) {
            return "byte";
        } else if (type.getSort() == SHORT) {
            return "short";
        } else if (type.getSort() == INT) {
            return "int";
        } else if (type.getSort() == FLOAT) {
            return "float";
        } else if (type.getSort() == LONG) {
            return "long";
        } else if (type.getSort() == DOUBLE) {
            return "double";
        } else if (type.getSort() == ARRAY) {
            return toClassName(type.getDescriptor());
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static MethodKey parseMethodKey(Map<Object, Object> map, String classInternalName) {
        var nameUntyped = map.remove("name");
        if (!(nameUntyped instanceof String)) {
            return null;
        }
        var name = (String) nameUntyped;
        if (name.isEmpty()) {
            return null;
        }

        var parameterTypesUntyped = map.remove("parameterTypes");
        if (!(parameterTypesUntyped instanceof List)) {
            logger.log(
                IN_TEST ? LogLevel.WARN : LogLevel.DEBUG,
                "`parameterTypes` are not set for method {} of {}",
                name,
                classInternalName
            );
            return null;
        }
        var parameterTypes = (List<Object>) parameterTypesUntyped;
        if (parameterTypes.isEmpty()) {
            return methodKeyOf(name, "()");
        }

        var asmTypes = new Type[parameterTypes.size()];
        for (int i = 0; i < asmTypes.length; i++) {
            var parameterType = parameterTypes.get(i).toString();
            var asmType = paramTypeToType(parameterType);
            asmTypes[i] = asmType;
        }

        var descriptor = getMethodDescriptor(VOID_TYPE, asmTypes);
        return methodKeyOf(name, descriptor);
    }

    private static Type paramTypeToType(String paramType) {
        if (paramType.startsWith("[")) {
            return getType(toClassInternalName(paramType));
        } else if (paramType.equals("boolean")) {
            return BOOLEAN_TYPE;
        } else if (paramType.equals("char")) {
            return CHAR_TYPE;
        } else if (paramType.equals("byte")) {
            return BYTE_TYPE;
        } else if (paramType.equals("short")) {
            return SHORT_TYPE;
        } else if (paramType.equals("int")) {
            return INT_TYPE;
        } else if (paramType.equals("float")) {
            return FLOAT_TYPE;
        } else if (paramType.equals("long")) {
            return LONG_TYPE;
        } else if (paramType.equals("double")) {
            return DOUBLE_TYPE;
        } else {
            return getType('L' + toClassInternalName(paramType) + ';');
        }
    }

    @Nullable
    private static Object remove(Map<?, ?> map, String... keys) {
        Object result = null;
        for (var key : keys) {
            var value = map.remove(key);
            if (value != null) {
                result = value;
            }
        }
        return result;
    }

    private static boolean removeBool(Map<?, ?> map, String... keys) {
        boolean result = false;
        for (var key : keys) {
            var value = map.remove(key);
            if (value != null) {
                var boolValue = parseBoolean(value.toString());
                if (boolValue) {
                    result = true;
                }
            }
        }
        return result;
    }

}
