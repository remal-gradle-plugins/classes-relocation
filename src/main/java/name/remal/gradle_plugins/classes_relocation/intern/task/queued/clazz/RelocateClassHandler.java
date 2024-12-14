package name.remal.gradle_plugins.classes_relocation.intern.task.queued.clazz;

import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;

import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;
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

            val relocationSource = getRelocationSource(resource, context);
            classVisitor = new RelocationAnnotationsClassVisitor(classVisitor, relocationSource);

            val remapper = new RelocationRemapper(context, classInternalName);
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

    @Nullable
    private static String getRelocationSource(Resource resource, RelocationContext context) {
        val classpathElement = resource.getClasspathElement();
        if (classpathElement == null) {
            return null;
        }

        val moduleIdentifier = context.getModuleIdentifier(resource);
        if (isNotEmpty(moduleIdentifier)) {
            return moduleIdentifier;
        }

        return classpathElement.getModuleName();
    }

}
