package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocationException;

public class UnsupportedClasspathPathException extends ClassesRelocationException {

    UnsupportedClasspathPathException(String message) {
        super(message);
    }

}
