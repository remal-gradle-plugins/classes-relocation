package name.remal.gradle_plugins.classes_relocation.intern;

import static lombok.AccessLevel.PRIVATE;

import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.annotations.ReliesOnInternalGradleApi;
import org.jetbrains.annotations.VisibleForTesting;

@NoArgsConstructor(access = PRIVATE)
abstract class InternalGradleConstantTimeForZipEntries {

    public static long getInternalGradleConstantTimeForZipEntriesMillis(long defaultValueMillis) {
        Long result = getInternalGradleConstantTimeForZipEntriesMillis(false);
        if (result == null) {
            return defaultValueMillis;
        }
        return result;
    }

    @Nullable
    @VisibleForTesting
    @SneakyThrows
    @ReliesOnInternalGradleApi
    static Long getInternalGradleConstantTimeForZipEntriesMillis(boolean rethrowExceptions) {
        try {
            val constantsClass = Class.forName("org.gradle.api.internal.file.archive.ZipEntryConstants");
            val timeField = constantsClass.getField("CONSTANT_TIME_FOR_ZIP_ENTRIES");
            return timeField.getLong(constantsClass);

        } catch (Exception e) {
            if (rethrowExceptions) {
                throw e;
            }
            return null;
        }
    }

}
