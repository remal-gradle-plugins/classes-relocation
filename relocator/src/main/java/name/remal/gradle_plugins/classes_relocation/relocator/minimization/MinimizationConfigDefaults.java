package name.remal.gradle_plugins.classes_relocation.relocator.minimization;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class MinimizationConfigDefaults {

    public static final List<String> KEEP_ANNOTATION_INCLUSIONS_DEFAULT = List.of(
        "jakarta.inject.**",
        "javax.inject.**",
        "com.fasterxml.jackson.**",
        "com.google.gson.**",
        "jakarta.validation.**",
        "javax.validation.**",
        "org.hibernate.validator.**"
    );

}
