package name.remal.gradle_plugins.classes_relocation.relocator;

import static lombok.AccessLevel.PRIVATE;

import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
        Throwable exception;

        try {
            var constantsClass = Class.forName("org.gradle.api.internal.file.archive.ZipEntryConstants");
            var timeField = constantsClass.getField("CONSTANT_TIME_FOR_ZIP_ENTRIES");
            return timeField.getLong(constantsClass);
        } catch (Exception e) {
            exception = e;
        }

        try {
            var constantsClass = Class.forName("org.gradle.api.internal.file.archive.ZipCopyAction");
            var timeField = constantsClass.getField("CONSTANT_TIME_FOR_ZIP_ENTRIES");
            return timeField.getLong(constantsClass);
        } catch (Exception e) {
            e.addSuppressed(exception);
            exception = e;
        }

        if (rethrowExceptions) {
            throw exception;
        }
        return null;
    }


}
