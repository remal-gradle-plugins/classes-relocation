package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.io.ByteStreams.toByteArray;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.val;

public interface Resource extends Comparable<Resource> {

    String getName();

    @Nullable
    Integer getMultiReleaseVersion();

    long getLastModifiedMillis();

    @MustBeClosed
    InputStream open() throws IOException;

    default byte[] readAllBytes() throws IOException {
        try (val in = open()) {
            return toByteArray(in);
        }
    }


    @Nullable
    ClasspathElement getClasspathElement();


    @Override
    default int compareTo(Resource other) {
        int result = getName().compareTo(other.getName());
        if (result == 0) {
            val thisVersion = getMultiReleaseVersion();
            val otherVersion = other.getMultiReleaseVersion();
            if (thisVersion != null && otherVersion != null) {
                result = thisVersion.compareTo(otherVersion);
            } else if (thisVersion != null) {
                result = 1;
            } else if (otherVersion != null) {
                result = -1;
            }
        }
        return result;
    }

}
