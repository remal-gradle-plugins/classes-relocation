package name.remal.gradle_plugins.classes_relocation.intern.asm;

import name.remal.gradle_plugins.classes_relocation.ClassesRelocationException;

public class UnsupportedAnnotationException extends ClassesRelocationException {

    UnsupportedAnnotationException(String message) {
        super(message);
    }

}
