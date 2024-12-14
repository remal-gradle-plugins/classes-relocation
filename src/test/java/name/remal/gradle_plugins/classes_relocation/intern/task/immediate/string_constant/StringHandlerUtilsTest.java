package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

class StringHandlerUtilsTest {

    @Test
    void isClassName() {
        ImmutableMap.<String, Boolean>builder()
            .put("Class", true)
            .put("p.C", true)
            .put("p/C", false)
            .put("pkg.Class", true)
            .put("pkg/Class", false)
            .put("pkg.Class$Sub", true)
            .put("pkg/Class$Sub", false)
            .put("pkg.sub.Class", true)
            .put("pkg/sub/Class", false)
            .put("pkg/sub.Class", false)
            .put(".pkg.Class", false)
            .put("/pkg/Class", false)
            .put("pkg.Class.", false)
            .put("pkg/Class/", false)
            .put("pkg..Class", false)
            .put("pkg//Class", false)
            .put("Lpkg/Class;", false)
            .put("[Lpkg/Class;", false)
            .put("", false)
            .build()
            .forEach((string, expected) ->
                assertEquals(expected, StringHandlerUtils.isClassName(string), string)
            );
    }

    @Test
    void isClassInternalName() {
        ImmutableMap.<String, Boolean>builder()
            .put("Class", true)
            .put("p.C", false)
            .put("p/C", true)
            .put("pkg.Class", false)
            .put("pkg/Class", true)
            .put("pkg.Class$Sub", false)
            .put("pkg/Class$Sub", true)
            .put("pkg.sub.Class", false)
            .put("pkg/sub/Class", true)
            .put("pkg/sub.Class", false)
            .put(".pkg.Class", false)
            .put("/pkg/Class", false)
            .put("pkg.Class.", false)
            .put("pkg/Class/", false)
            .put("pkg..Class", false)
            .put("pkg//Class", false)
            .put("Lpkg/Class;", false)
            .put("[Lpkg/Class;", false)
            .put("", false)
            .build()
            .forEach((string, expected) ->
                assertEquals(expected, StringHandlerUtils.isClassInternalName(string), string)
            );
    }

}
