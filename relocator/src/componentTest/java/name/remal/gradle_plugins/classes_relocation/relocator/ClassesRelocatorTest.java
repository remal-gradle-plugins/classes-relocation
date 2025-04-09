package name.remal.gradle_plugins.classes_relocation.relocator;

import name.remal.gradle_plugins.classes_relocation.relocator.accessible_field_from_parent_class.AccessibleFieldFromParentClassTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.annotated_with_inject.AnnotatedWithInjectTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.classgraph.ClassgraphTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.guava_class_descriptor.GuavaClassDescriptorTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.guava_class_name.GuavaClassNameTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.guava_immutable_list.GuavaImmutableListTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.guava_internal_class_name.GuavaInternalClassNameTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.jackson_guava.JacksonGuavaTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.jackson_java_time.JacksonJavaTimeTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.joda_time_zone_info.JodaTimeZoneInfoTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent.RelocatedParentMethodTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.relocated_constructors.RelocatedConstructorsTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.saxon_catalogs.SaxonCatalogsTestLogic;
import org.junit.jupiter.api.Test;

class ClassesRelocatorTest extends ClassesRelocatorTestBase {

    @Test
    void relocatedConstructors() {
        assertTestLogic(
            RelocatedConstructorsTestLogic.class
        );
    }

    @Test
    void annotatedWithInject() {
        assertTestLogic(
            AnnotatedWithInjectTestLogic.class
        );
    }

    @Test
    void accessibleFieldFromParentClass() {
        assertTestLogic(
            AccessibleFieldFromParentClassTestLogic.class
        );
    }


    @Test
    void simple() {
        assertTestLogic(
            GuavaImmutableListTestLogic.class,
            "com.google.guava:guava"
        );
    }

    @Test
    void className() {
        assertTestLogic(
            GuavaClassNameTestLogic.class,
            "com.google.guava:guava"
        );
    }

    @Test
    void internalClassName() {
        assertTestLogic(
            GuavaInternalClassNameTestLogic.class,
            "com.google.guava:guava"
        );
    }

    @Test
    void classDescriptor() {
        assertTestLogic(
            GuavaClassDescriptorTestLogic.class,
            "com.google.guava:guava"
        );
    }

    @Test
    void metaInfServicesRelocated() {
        assertTestLogic(
            JacksonGuavaTestLogic.class,
            "com.fasterxml.jackson.core:jackson-databind",
            "com.fasterxml.jackson.datatype:jackson-datatype-guava"
        );
    }

    @Test
    void metaInfServicesMerged() {
        /*
         * JavaTime will be the last library on the relocation classpath.
         * This test will succeed only if META-INF/services are merged.
         */
        assertTestLogic(
            JacksonJavaTimeTestLogic.class,
            "com.fasterxml.jackson.core:jackson-databind",
            "com.fasterxml.jackson.datatype:jackson-datatype-guava",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
        );
    }

    @Test
    void resourcesRelocatedBasedOnPathPrefix() {
        assertTestLogic(
            JodaTimeZoneInfoTestLogic.class,
            "joda-time:joda-time"
        );
    }

    @Test
    void xmlFiles() {
        assertTestLogic(
            SaxonCatalogsTestLogic.class,
            "net.sf.saxon:Saxon-HE"
        );
    }

    @Test
    void classgraph() {
        assertTestLogic(
            ClassgraphTestLogic.class,
            "io.github.classgraph:classgraph"
        );
    }

    @Test
    void relocatedParentMethod() {
        assertTestLogic(
            RelocatedParentMethodTestLogic.class
        );
    }

}
