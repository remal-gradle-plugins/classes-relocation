package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class StringHandlerUtilsTest {

    @Test
    void isClassName() {
        ImmutableMap.<String, Boolean>builder()
            .put(StringHandlerUtilsTest.class.getName(), true)
            .put(toClassInternalName(StringHandlerUtilsTest.class.getName()), false)
            .put('.' + StringHandlerUtilsTest.class.getName(), false)
            .put(toClassInternalName('.' + StringHandlerUtilsTest.class.getName()), false)
            .put(StringHandlerUtilsTest.class.getName() + '.', false)
            .put(toClassInternalName(StringHandlerUtilsTest.class.getName() + '.'), false)
            .put(Type.getDescriptor(StringHandlerUtilsTest.class), false)
            .put(";", false)
            .put("[", false)
            .put("", false)
            .build()
            .forEach((string, expected) ->
                assertEquals(expected, StringHandlerUtils.isClassName(string), string)
            );
    }

    @Test
    void isClassInternalName() {
        ImmutableMap.<String, Boolean>builder()
            .put(StringHandlerUtilsTest.class.getName(), false)
            .put(toClassInternalName(StringHandlerUtilsTest.class.getName()), true)
            .put('.' + StringHandlerUtilsTest.class.getName(), false)
            .put(toClassInternalName('.' + StringHandlerUtilsTest.class.getName()), false)
            .put(StringHandlerUtilsTest.class.getName() + '.', false)
            .put(toClassInternalName(StringHandlerUtilsTest.class.getName() + '.'), false)
            .put(Type.getDescriptor(StringHandlerUtilsTest.class), false)
            .put(";", false)
            .put("[", false)
            .put("", false)
            .build()
            .forEach((string, expected) ->
                assertEquals(expected, StringHandlerUtils.isClassInternalName(string), string)
            );
    }

}
