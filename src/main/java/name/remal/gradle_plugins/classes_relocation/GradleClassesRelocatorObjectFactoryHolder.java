package name.remal.gradle_plugins.classes_relocation;

import static lombok.AccessLevel.PRIVATE;

import javax.inject.Inject;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorObjectFactory;
import org.gradle.api.model.ObjectFactory;

@NoArgsConstructor(access = PRIVATE)
abstract class GradleClassesRelocatorObjectFactoryHolder {

    public abstract static class GradleClassesRelocatorObjectFactory implements ClassesRelocatorObjectFactory {

        @Override
        public <T> T create(Class<T> clazz) {
            return getObjects().newInstance(clazz);
        }

        @Inject
        protected abstract ObjectFactory getObjects();

    }

}
