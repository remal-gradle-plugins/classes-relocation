package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
abstract class ClasspathUtils {

    public static final long MAX_ARCHIVE_FILE_SIZE_TO_LOAD_IN_HEAP_BYTES = 5 * 1024 * 1024L;

}
