package name.remal.gradle_plugins.classes_relocation.relocator.joda_time_zone_info;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import org.joda.time.tz.ZoneInfoProvider;

public class JodaTimeZoneInfoTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        val zoneInfoProvider = new ZoneInfoProvider();
        assertDoesNotThrow(() -> zoneInfoProvider.getZone("Africa/Dakar"));
        assertDoesNotThrow(() -> zoneInfoProvider.getZone("America/Los_Angeles"));
        assertDoesNotThrow(() -> zoneInfoProvider.getZone("Asia/Dubai"));
        assertDoesNotThrow(() -> zoneInfoProvider.getZone("Etc/GMT"));
        assertDoesNotThrow(() -> zoneInfoProvider.getZone("Europe/Warsaw"));
    }

}
