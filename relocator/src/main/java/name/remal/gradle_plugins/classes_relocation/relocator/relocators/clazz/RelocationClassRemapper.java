package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

class RelocationClassRemapper extends ClassRemapper {

    private final RelocationContext context;

    public RelocationClassRemapper(@Nullable ClassVisitor classVisitor, Remapper remapper, RelocationContext context) {
        super(classVisitor, remapper);
        this.context = context;
    }

    private final AtomicReference<String> currentFieldName = new AtomicReference<>();

    @Override
    public FieldVisitor visitField(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable Object value
    ) {
        currentFieldName.set(name);
        try {
            return super.visitField(access, name, descriptor, signature, value);
        } finally {
            currentFieldName.set(null);
        }
    }

    @Override
    protected FieldVisitor createFieldRemapper(FieldVisitor fieldVisitor) {
        var remapper = this.remapper;
        var context = this.context;
        if (remapper instanceof RelocationRemapper) {
            var currentFieldName = this.currentFieldName.get();
            if (className != null && currentFieldName != null) {
                context = context.getRelocationComponent(ReachabilityReport.class)
                    .field(className, currentFieldName)
                    .wrapRelocationContext(context);
                remapper = ((RelocationRemapper) remapper).withContext(context);
            }
        }
        return new RelocationFieldRemapper(api, fieldVisitor, remapper);
    }


    private final AtomicReference<MethodKey> currentMethodKey = new AtomicReference<>();

    @Override
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions
    ) {
        currentMethodKey.set(methodKeyOf(name, descriptor));
        try {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        } finally {
            currentMethodKey.set(null);
        }
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        var remapper = this.remapper;
        var context = this.context;
        if (remapper instanceof RelocationRemapper) {
            var currentMethodKey = this.currentMethodKey.get();
            if (className != null && currentMethodKey != null) {
                context = context.getRelocationComponent(ReachabilityReport.class)
                    .method(className, currentMethodKey)
                    .wrapRelocationContext(context);
                remapper = ((RelocationRemapper) remapper).withContext(context);
            }
        }
        return new RelocationMethodRemapper(api, methodVisitor, remapper, className, context);
    }

}
