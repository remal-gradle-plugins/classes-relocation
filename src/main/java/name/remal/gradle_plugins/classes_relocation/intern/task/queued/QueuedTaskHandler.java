package name.remal.gradle_plugins.classes_relocation.intern.task.queued;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.BaseTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.BaseWithSupportedTaskClass;

public interface QueuedTaskHandler<TASK extends QueuedTask>
    extends BaseTaskHandler, BaseWithSupportedTaskClass<TASK> {

    @Override
    default Class<TASK> getSupportedTaskClass() {
        val handlerType = TypeToken.of(getClass()).getSupertype(QueuedTaskHandler.class).getType();
        if (!(handlerType instanceof ParameterizedType)) {
            throw new IllegalStateException("Not ParameterizedType: " + getClass());
        }
        val handlerSupportedTaskType = ((ParameterizedType) handlerType).getActualTypeArguments()[0];
        @SuppressWarnings("unchecked")
        val handlerSupportedTaskClass = (Class<TASK>) TypeToken.of(handlerSupportedTaskType).getRawType();
        return handlerSupportedTaskClass;
    }

    QueuedTaskHandlerResult handle(TASK task, RelocationContext context);

    default void postProcess(RelocationContext context) {
        // do nothing by default
    }

}
