package github.com.gengyoubo.replayneo.core.input;

import github.com.gengyoubo.replayneo.api.input.ReplayKeyInput;

public record DefaultReplayKeyInput(int keyCode, int scanCode, int modifiers) implements ReplayKeyInput {
}
