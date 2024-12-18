package name.remal.gradle_plugins.classes_relocation;

public enum SourceSetClasspathsCheckMode {

    /**
     * Throw an exception
     */
    FAIL,

    /**
     * Log a warning message
     */
    WARN,

    /**
     * Disable the check
     */
    DISABLE,

}
