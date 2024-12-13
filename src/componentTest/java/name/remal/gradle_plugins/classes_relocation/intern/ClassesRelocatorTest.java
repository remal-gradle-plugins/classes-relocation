package name.remal.gradle_plugins.classes_relocation.intern;

import name.remal.gradle_plugins.classes_relocation.intern.guava_class_descriptor.GuavaClassDescriptorTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_class_name.GuavaClassNameTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_immutable_list.GuavaImmutableListTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_internal_class_name.GuavaInternalClassNameTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.jackson_guava.JacksonGuavaTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.jackson_java_time.JacksonJavaTimeTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.joda_time_zone_info.JodaTimeZoneInfoTestLogic;
import org.junit.jupiter.api.Test;

class ClassesRelocatorTest extends ClassesRelocatorTestBase {

    @Test
    void simple() {
        assertTestLogic(GuavaImmutableListTestLogic.class, "guava");
    }

    @Test
    void className() {
        assertTestLogic(GuavaClassNameTestLogic.class, "guava");
    }

    @Test
    void internalClassName() {
        assertTestLogic(GuavaInternalClassNameTestLogic.class, "guava");
    }

    @Test
    void classDescriptor() {
        assertTestLogic(GuavaClassDescriptorTestLogic.class, "guava");
    }

    @Test
    void metaInfServicesRelocated() {
        assertTestLogic(JacksonGuavaTestLogic.class, "jackson-databind", "jackson-guava");
    }

    @Test
    void metaInfServicesMerged() {
        /*
         * JavaTime will be the last library on the relocation classpath.
         * This test will succeed only if META-INF/services are merged.
         */
        assertTestLogic(JacksonJavaTimeTestLogic.class, "jackson-databind", "jackson-guava", "jackson-jsr310");
    }

    @Test
    void resourcesRelocatedBasedOnPathPrefix() {
        assertTestLogic(JodaTimeZoneInfoTestLogic.class, "joda-time");
    }

}
