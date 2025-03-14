package name.remal.gradle_plugins.classes_relocation.relocator.jackson_java_time;

import static com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

public class JacksonJavaTimeTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        var objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .build();

        var instant = Instant.parse("2024-11-13T17:03:05Z");

        // serialization:
        assertEquals(
            "\"" + instant + "\"",
            objectMapper.writeValueAsString(instant)
        );

        // deserialization:
        assertEquals(
            instant,
            objectMapper.readValue("\"" + instant + "\"", Instant.class)
        );
        assertEquals(
            instant,
            objectMapper.readValue(String.valueOf(instant.toEpochMilli()), Instant.class)
        );
    }

}
