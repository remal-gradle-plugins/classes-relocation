package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmTestUtils.wrapWithTestClassVisitors;

import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

public class RelocateClassHandler implements QueuedTaskHandler<RelocateClass> {

    @Override
    @SneakyThrows
    @SuppressWarnings({"java:S1121", "VariableDeclarationUsageDistance"})
    public QueuedTaskHandlerResult handle(RelocateClass task, RelocationContext context) {
        val classInternalName = task.getClassInternalName();
        val classResources = context.getRelocationClasspath().getClassResources(classInternalName);
        for (val resource : classResources) {
            ClassVisitor classVisitor;
            val classWriter = (ClassWriter) (classVisitor = new ClassWriter(0));

            classVisitor = wrapWithTestClassVisitors(classVisitor);

            val relocatedNameVisitor = (NameClassVisitor) (classVisitor = new NameClassVisitor(classVisitor));

            val relocationSource = context.getRelocationSource(resource);
            classVisitor = new RelocationAnnotationsClassVisitor(classVisitor, relocationSource);

            val remapper = new RelocationRemapper(classInternalName, resource, context);
            classVisitor = new ClassRemapper(classVisitor, remapper);

            try (val in = resource.open()) {
                new ClassReader(in).accept(classVisitor, 0);
            }

            context.writeToOutput(
                resource,
                relocatedNameVisitor.getClassInternalName() + ".class",
                classWriter.toByteArray()
            );
        }

        return TASK_HANDLED;
    }

}
