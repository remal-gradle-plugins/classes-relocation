package name.remal.gradle_plugins.classes_relocation.intern.task.immediate;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.Ordered;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.BaseWithSupportedTaskClass;

public interface ImmediateTaskHandler<RESULT, TASK extends ImmediateTask<RESULT>>
    extends Ordered, BaseWithSupportedTaskClass<TASK> {

    @Override
    default Class<TASK> getSupportedTaskClass() {
        val handlerType = TypeToken.of(getClass()).getSupertype(ImmediateTaskHandler.class).getType();
        if (!(handlerType instanceof ParameterizedType)) {
            throw new IllegalStateException("Not ParameterizedType: " + getClass());
        }
        val handlerSupportedTaskType = ((ParameterizedType) handlerType).getActualTypeArguments()[1];
        @SuppressWarnings("unchecked")
        val handlerSupportedTaskClass = (Class<TASK>) TypeToken.of(handlerSupportedTaskType).getRawType();
        return handlerSupportedTaskClass;
    }

    Optional<RESULT> handle(TASK task, RelocationContext context) throws Throwable;

}
