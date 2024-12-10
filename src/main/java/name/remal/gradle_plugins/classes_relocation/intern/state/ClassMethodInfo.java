package name.remal.gradle_plugins.classes_relocation.intern.state;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toMethodParamsDescriptor;

import com.google.errorprone.annotations.DoNotCall;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE, onConstructor_ = {@DoNotCall})
public class ClassMethodInfo {

    static ClassMethodInfo newMethodInfo(boolean instanceMethod, String methodName, String methodDescriptor) {
        return new ClassMethodInfo(
            instanceMethod,
            methodName,
            toMethodParamsDescriptor(methodDescriptor)
        );
    }


    boolean instanceMethod;

    String methodName;

    String methodParamsDescriptor;

}
