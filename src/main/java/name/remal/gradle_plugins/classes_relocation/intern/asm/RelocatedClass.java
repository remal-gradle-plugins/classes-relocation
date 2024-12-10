package name.remal.gradle_plugins.classes_relocation.intern.asm;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated class was relocated.
 * Optionally, the relocation source can be specified.
 */
@Target(TYPE)
@Retention(CLASS)
@interface RelocatedClass {
    String source() default "";
}
