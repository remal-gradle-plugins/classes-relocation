package name.remal.gradle_plugins.classes_relocation.relocator.task;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorLifecycleComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorOrderedComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;

public interface ImmediateTaskHandler<RESULT, TASK extends ImmediateTask<RESULT>>
    extends ClassesRelocatorOrderedComponent, ClassesRelocatorLifecycleComponent, BaseWithSupportedTaskClass<TASK> {

    @Override
    default Class<TASK> getSupportedTaskClass() {
        var handlerType = TypeToken.of(getClass()).getSupertype(ImmediateTaskHandler.class).getType();
        if (!(handlerType instanceof ParameterizedType)) {
            throw new IllegalStateException("Not ParameterizedType: " + getClass());
        }
        var handlerSupportedTaskType = ((ParameterizedType) handlerType).getActualTypeArguments()[1];
        @SuppressWarnings("unchecked")
        var handlerSupportedTaskClass = (Class<TASK>) TypeToken.of(handlerSupportedTaskType).getRawType();
        return handlerSupportedTaskClass;
    }

    Optional<RESULT> handle(TASK task, RelocationContext context) throws Throwable;

}
