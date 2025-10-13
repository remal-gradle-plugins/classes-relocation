package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;
import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;

import java.util.function.Consumer;
import name.remal.gradle_plugins.toolkit.LateInit;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;

public class NameClassVisitor extends ClassVisitor {

    private final LateInit<String> classInternalName = lateInit("classInternalName");

    @Nullable
    private final Consumer<String> classInternalNameConsumer;

    public NameClassVisitor(
        ClassVisitor classVisitor,
        @Nullable Consumer<String> classInternalNameConsumer
    ) {
        super(getLatestAsmApi(), classVisitor);
        this.classInternalNameConsumer = classInternalNameConsumer;
    }

    public NameClassVisitor(ClassVisitor classVisitor) {
        this(classVisitor, null);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        @Nullable String signature,
        @Nullable String superName,
        String @Nullable [] interfaces
    ) {
        classInternalName.set(name);
        if (classInternalNameConsumer != null) {
            classInternalNameConsumer.accept(name);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public String getClassInternalName() {
        return classInternalName.get();
    }

}
