package name.remal.gradle_plugins.classes_relocation.intern.task.class_relocation;

import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInUnitTest;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

@RequiredArgsConstructor
class RelocateSourceClassTask implements RelocationTask {

    private static final boolean IN_TEST = isInUnitTest();


    private final Resource sourceClassResource;

    @Override
    public int getPriority() {
        return PROCESS_SOURCE_PRIORITY;
    }

    @Override
    public void execute(RelocationContext context) throws Exception {
        val classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);

        ClassVisitor classVisitor = new RelocateSourceClassRemapper(context, classWriter);

        if (IN_TEST) {
            classVisitor = wrapWithTestClassVisitors(classVisitor);
        }

        try (val in = sourceClassResource.open()) {
            new ClassReader(in).accept(classVisitor, 0);
        }

        val bytes = classWriter.toByteArray();
        context.targetEntryFor(sourceClassResource).write(bytes);
    }

}
