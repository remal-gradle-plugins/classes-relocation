package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;

import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.asm.UnsupportedAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

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
            classVisitor = new RelocationClassRemapper(classVisitor, remapper, context);

            classVisitor = new UnsupportedAnnotationsClassVisitor(classVisitor,
                "Lname/remal/gradle_plugins/api/RelocateClasses;",
                "Lname/remal/gradle_plugins/api/RelocatePackages;"
            );

            try (val in = resource.open()) {
                new ClassReader(in).accept(classVisitor, 0);
            }

            val newResource = newGeneratedResource(builder -> builder
                .withSourceResource(resource)
                .withName(relocatedNameVisitor.getClassInternalName() + ".class")
                .withContent(classWriter.toByteArray())
            );
            context.writeToOutput(newResource);
        }

        return TASK_HANDLED;
    }

}
