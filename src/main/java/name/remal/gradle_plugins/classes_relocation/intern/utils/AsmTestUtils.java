package name.remal.gradle_plugins.classes_relocation.intern.utils;

import static lombok.AccessLevel.PRIVATE;

import java.io.PrintWriter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * <b>DO NOT USE IT UNLESS YOU KNOW WHAT YOU ARE DOING!</b>
 *
 * <p>{@link CheckClassAdapter} is an optional dependency, so it's reachability should be in a separate class.
 */
@ApiStatus.Internal
@NoArgsConstructor(access = PRIVATE)
public abstract class AsmTestUtils {

    private static final boolean TRACE = false;

    @SuppressWarnings({"java:S106", "DefaultCharset"})
    public static ClassVisitor wrapWithTestClassVisitors(ClassVisitor classVisitor) {
        if (TRACE) {
            val writer = new PrintWriter(System.out, true);
            classVisitor = new TraceClassVisitor(classVisitor, writer);
        }

        classVisitor = new CheckClassAdapter(classVisitor);

        return classVisitor;
    }

}
