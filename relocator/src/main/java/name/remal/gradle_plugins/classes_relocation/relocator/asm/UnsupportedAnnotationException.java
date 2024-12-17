package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocationException;

public class UnsupportedAnnotationException extends ClassesRelocationException {

    UnsupportedAnnotationException(String message) {
        super(message);
    }

}
