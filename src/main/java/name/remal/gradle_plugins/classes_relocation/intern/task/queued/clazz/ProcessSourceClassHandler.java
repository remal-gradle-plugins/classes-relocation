package name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz;

import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static org.objectweb.asm.Type.getDescriptor;

import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.api.RelocateClasses;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.UnsupportedAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;
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

            val remapper = new RelocationRemapper(context, toClassInternalName(task.getSourceClassName()));
            classVisitor = new ClassRemapper(classVisitor, remapper);

            classVisitor = new UnsupportedAnnotationsClassVisitor(classVisitor,
                getDescriptor(RelocateClasses.class), // TODO: implement it instead of throwing an exception
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
