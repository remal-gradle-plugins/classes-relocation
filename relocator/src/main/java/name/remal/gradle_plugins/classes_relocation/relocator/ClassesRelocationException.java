package name.remal.gradle_plugins.classes_relocation.relocator;

import org.jetbrains.annotations.ApiStatus;

public class ClassesRelocationException extends RuntimeException {

    @ApiStatus.Internal
    public ClassesRelocationException(String message) {
        super(message);
    }

    @ApiStatus.Internal
    public ClassesRelocationException(String message, Throwable cause) {
        super(message, cause);
    }

}
