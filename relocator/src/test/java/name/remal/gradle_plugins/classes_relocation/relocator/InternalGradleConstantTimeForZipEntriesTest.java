package name.remal.gradle_plugins.classes_relocation.relocator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class InternalGradleConstantTimeForZipEntriesTest {

    @Test
    void doesNotThrowExceptions() {
        assertDoesNotThrow(() ->
            InternalGradleConstantTimeForZipEntries.getInternalGradleConstantTimeForZipEntriesMillis(true)
        );
    }

}
