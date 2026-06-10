package github.com.gengyoubo.replayneo.core.function;

import github.com.gengyoubo.replayneo.api.function.CharInput;

public record CharacterInput(char character, int modifiers) implements CharInput {
}
