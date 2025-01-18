package name.remal.gradle_plugins.classes_relocation.relocator;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.isPackagePrivate;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.isPrivate;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
class ClassesRelocatorObjectFactoryDefault implements ClassesRelocatorObjectFactory {

    public static final ClassesRelocatorObjectFactoryDefault DEFAULT_OBJECT_FACTORY =
        new ClassesRelocatorObjectFactoryDefault();

    @Override
    public <T> T create(Class<T> clazz) throws Throwable {
        var ctor = clazz.getDeclaredConstructor();
        if (isPrivate(ctor)) {
            throw new AssertionError("Can't use private constructor for instantiation: " + ctor);
        } else if (isPackagePrivate(ctor)) {
            throw new AssertionError("Can't use package-private constructor for instantiation: " + ctor);
        }
        return makeAccessible(ctor).newInstance();
    }

}
