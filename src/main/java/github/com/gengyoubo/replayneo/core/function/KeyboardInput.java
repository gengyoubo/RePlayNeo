package github.com.gengyoubo.replayneo.core.function;

import github.com.gengyoubo.replayneo.api.function.KeyInput;

public record KeyboardInput(int key, int scancode, int modifiers) implements KeyInput {
}
