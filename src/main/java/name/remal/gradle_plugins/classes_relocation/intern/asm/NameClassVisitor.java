package name.remal.gradle_plugins.classes_relocation.intern.asm;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.getLatestAsmApi;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;

import javax.annotation.Nullable;
import name.remal.gradle_plugins.toolkit.LateInit;
import org.objectweb.asm.ClassVisitor;

public class NameClassVisitor extends ClassVisitor {

    public NameClassVisitor(ClassVisitor classVisitor) {
        super(getLatestAsmApi(), classVisitor);
    }

    private final LateInit<String> className = lateInit("className");

    @Override
    public void visit(
        int version,
        int access,
        String name,
        @Nullable String signature,
        @Nullable String superName,
        @Nullable String[] interfaces
    ) {
        className.set(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public String getClassInternalName() {
        return className.get();
    }

}
