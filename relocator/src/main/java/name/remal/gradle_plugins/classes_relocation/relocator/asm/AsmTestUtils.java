package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInFunctionalTest;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;

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

    private static final boolean IN_TEST = isInTest()
        && !isInFunctionalTest(); // we don't have org.ow2.asm:asm-util in functional tests

    private static final boolean TRACE = false;


    public static ClassVisitor wrapWithTestClassVisitors(ClassVisitor classVisitor) {
        if (IN_TEST) {
            if (TRACE) {
                classVisitor = TraceClassVisitorApplier.wrap(classVisitor);
            }
            classVisitor = CheckClassAdapterApplier.wrap(classVisitor);
        }

        return classVisitor;
    }

    private static class CheckClassAdapterApplier {

        public static ClassVisitor wrap(ClassVisitor classVisitor) {
            return new CheckClassAdapter(classVisitor);
        }

    }

    private static class TraceClassVisitorApplier {

        @SuppressWarnings({"java:S106", "DefaultCharset"})
        public static ClassVisitor wrap(ClassVisitor classVisitor) {
            val writer = new PrintWriter(System.out, true);
            return new TraceClassVisitor(classVisitor, writer);
        }

    }
}
