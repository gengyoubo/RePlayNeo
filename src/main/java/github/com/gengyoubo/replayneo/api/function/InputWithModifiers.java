package github.com.gengyoubo.replayneo.api.function;

import java.util.function.IntSupplier;

public interface InputWithModifiers {
    int modifiers();

    default boolean hasCtrl() {
        return (modifiers() & _CTRL_MOD) != 0;
    }
    default boolean hasShift() {
        return (modifiers() & _SHIFT_MOD) != 0;
    }
    default boolean hasAlt() {
        return (modifiers() & _ALT_MOD) != 0;
    }

    int _SHIFT_MOD = 0x0001;
    int _CTRL_MOD = 0x0002 | 0x0008;
    int _ALT_MOD = 0x0004;

    static int currentModifiers() {
        return CurrentModifiers.supplier.getAsInt();
    }

    static void setCurrentModifiersSupplier(IntSupplier supplier) {
        CurrentModifiers.supplier = supplier == null ? () -> 0 : supplier;
    }

    class CurrentModifiers {
        private static IntSupplier supplier = () -> 0;
    }
}
