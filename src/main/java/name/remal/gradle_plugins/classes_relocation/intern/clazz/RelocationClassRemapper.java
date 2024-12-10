package name.remal.gradle_plugins.classes_relocation.intern.clazz;

import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import javax.annotation.Nullable;
import lombok.Builder;
import name.remal.gradle_plugins.classes_relocation.intern.state.RelocationState;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public class RelocationClassRemapper extends ClassRemapper {

    private static final boolean IN_TEST = isInTest();


    private final RelocationState state;
    private final boolean withoutMembers;

    @Builder
    private RelocationClassRemapper(
        RelocationState state,
        ClassVisitor classVisitor,
        Remapper remapper,
        boolean withoutMembers
    ) {
        super(
            IN_TEST ? wrapWithTestClassVisitors(classVisitor) : classVisitor,
            remapper
        );
        this.state = state;
        this.withoutMembers = withoutMembers;
    }

    public String getInternalClassName() {
        return requireNonNull(className);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        @Nullable String signature,
        @Nullable String superName,
        @Nullable String[] interfaces
    ) {
        state.registerParentClass(name, superName);
        state.registerParentClasses(name, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lname/remal/gradle_plugins/api/RelocateClasses;")
            || descriptor.equals("Lname/remal/gradle_plugins/api/RelocatePackages;")
        ) {
            throw new UnsupportedOperationException("Not supported: " + descriptor);
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    @Nullable
    public FieldVisitor visitField(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable Object value
    ) {
        if (withoutMembers) {
            return null;
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions
    ) {
        if (withoutMembers) {
            return null;
        }

        if ((access & ACC_PRIVATE) == 0) {
            state.registerMethod(
                getInternalClassName(),
                (access & ACC_STATIC) == 0,
                name,
                descriptor
            );
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    @Nullable
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        return RelocationMethodRemapper.builder()
            .state(state)
            .api(api)
            .methodVisitor(methodVisitor)
            .remapper(remapper)
            .build();
    }

}
