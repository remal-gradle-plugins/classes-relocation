package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation prevents some static analysis and coverage tools from processing the annotated class.
 */
@Target(TYPE)
@Retention(CLASS)
@interface Generated {
}
