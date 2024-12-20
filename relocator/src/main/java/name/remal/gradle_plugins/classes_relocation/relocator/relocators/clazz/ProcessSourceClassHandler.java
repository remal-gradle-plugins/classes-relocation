package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;

import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.UnsupportedAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

public class ProcessSourceClassHandler implements QueuedTaskHandler<ProcessSourceClass> {

    @Override
    @SneakyThrows
    @SuppressWarnings({"java:S1121", "VariableDeclarationUsageDistance"})
    public QueuedTaskHandlerResult handle(ProcessSourceClass task, RelocationContext context) {
        val classResources = context.getSourceClasspath().getClassResources(task.getSourceClassName());
        for (val resource : classResources) {
            ClassVisitor classVisitor;
            val classWriter = (ClassWriter) (classVisitor = new ClassWriter(0));

            classVisitor = wrapWithTestClassVisitors(classVisitor);

            val relocatedNameVisitor = (NameClassVisitor) (classVisitor = new NameClassVisitor(classVisitor));

            val remapper = new RelocationRemapper(toClassInternalName(task.getSourceClassName()), resource, context);
            classVisitor = new ClassRemapper(classVisitor, remapper);

            classVisitor = new UnsupportedAnnotationsClassVisitor(classVisitor,
                "Lname/remal/gradle_plugins/api/RelocateClasses;",
                "Lname/remal/gradle_plugins/api/RelocatePackages;"
            );

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
