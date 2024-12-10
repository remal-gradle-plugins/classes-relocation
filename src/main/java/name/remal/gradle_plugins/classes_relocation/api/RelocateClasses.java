package name.remal.gradle_plugins.classes_relocation.api;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({TYPE, CONSTRUCTOR, METHOD})
@Retention(CLASS)
public @interface RelocateClasses {

    /**
     * Glob-style class name patterns of classes that should be relocated.
     *
     * <p>Examples:
     * <ul>
     * <li>{@code com.google.common.**} relocates all Guava classes.
     * <li>{@code com.google.common.base.*} relocates Guava's base classes
     * ({@code Preconditions}, {@code Joiner}, {@code Splitter}, etc).
     * <li>{@code com.google.common.base.Preconditions} relocates Guava's {@code Preconditions} class only.
     * </ul>
     */
    String[] value();

    /**
     * Required dependency classes from the same artifact file will be relocated unless they are excluded.
     *
     * <p>For example {@code com.google.common.reflect.TypeToken} will relocate not only {@code TypeToken},
     * but other Guava classes that {@code TypeToken} depends on
     * (like {@code Preconditions}, {@code Predicate}, {@code Primitives}, etc).
     */
    boolean includeDependenciesFromTheSameArtifact() default true;

    /**
     * Glob-style class name patterns of classes that should <b>not</b> be relocated.
     */
    String[] exclude() default {};

}
