package name.remal.gradle_plugins.classes_relocation.relocator;

public class ClassesRelocatorObjectInstantiationException extends RuntimeException {

    ClassesRelocatorObjectInstantiationException(String message) {
        super(message);
    }

    ClassesRelocatorObjectInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    ClassesRelocatorObjectInstantiationException(Throwable cause) {
        super(cause);
    }

}
