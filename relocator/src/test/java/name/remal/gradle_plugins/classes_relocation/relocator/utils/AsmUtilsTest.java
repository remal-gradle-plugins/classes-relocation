package name.remal.gradle_plugins.classes_relocation.relocator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.ASM9;

import name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils;
import org.junit.jupiter.api.Test;

class AsmUtilsTest {

    @Test
    void getLatestAsmApi() {
        assertEquals(ASM9, AsmUtils.getLatestAsmApi());
    }

}
