package name.remal.gradle_plugins.classes_relocation.relocator.utils;

import static java.lang.System.identityHashCode;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class ComparatorUtils {

    public static int compareClasses(Class<?> clazz1, Class<?> clazz2) {
        if (clazz1 == clazz2) {
            return 0;
        }

        int result = clazz1.getName().compareTo(clazz2.getName());
        if (result != 0) {
            return result;
        }

        result = Integer.compare(identityHashCode(clazz1), identityHashCode(clazz2));
        if (result != 0) {
            return result;
        }

        var classLoader1 = clazz1.getClassLoader();
        var classLoader2 = clazz2.getClassLoader();
        return Integer.compare(
            classLoader1 != null ? identityHashCode(classLoader1) : 0,
            classLoader2 != null ? identityHashCode(classLoader2) : 0
        );
    }

}
