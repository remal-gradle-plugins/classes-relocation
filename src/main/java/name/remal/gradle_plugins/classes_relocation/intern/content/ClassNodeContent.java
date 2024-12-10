package name.remal.gradle_plugins.classes_relocation.intern.content;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInUnitTest;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import lombok.val;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class ClassNodeContent implements MutableContentType<ClassNode> {

    private static final boolean IN_TEST = isInUnitTest();

    @Override
    public Class<ClassNode> getType() {
        return ClassNode.class;
    }

    @Override
    public ClassNode fromBytes(byte[] bytes) {
        val classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);
        return classNode;
    }

    @Override
    public byte[] toBytes(ClassNode classNode) {
        val classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);

        ClassVisitor classVisitor = classWriter;
        if (IN_TEST) {
            classVisitor = wrapWithTestClassVisitors(classVisitor);
        }

        classNode.accept(classVisitor);

        return classWriter.toByteArray();
    }

}
