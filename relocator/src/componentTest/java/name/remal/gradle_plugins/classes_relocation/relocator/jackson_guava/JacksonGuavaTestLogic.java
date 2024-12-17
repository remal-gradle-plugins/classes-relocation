package name.remal.gradle_plugins.classes_relocation.relocator.jackson_guava;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

public class JacksonGuavaTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        val objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

        // serialization:
        assertEquals(
            "[1]",
            objectMapper.writeValueAsString(ImmutableList.of(1))
        );

        // deserialization:
        assertEquals(
            ImmutableSet.of(2),
            objectMapper.readValue("[2]", ImmutableSet.class)
        );
    }

}
