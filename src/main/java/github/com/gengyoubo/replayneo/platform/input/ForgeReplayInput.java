package github.com.gengyoubo.replayneo.platform.input;

import github.com.gengyoubo.replayneo.api.input.ReplayInput;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBinding;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyHandler;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyInput;

public class ForgeReplayInput implements ReplayInput {
    private final ForgeKeyBindingRegistry keyBindingRegistry = new ForgeKeyBindingRegistry();

    @Override
    public ReplayKeyBindingRegistry keyBindingRegistry() {
        return keyBindingRegistry;
    }

    public ForgeKeyBindingRegistry forgeKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    @Override
    public ReplayKeyBinding registerKey(String id, int defaultKeyCode, boolean replayOnly) {
        ReplayKeyBindingRegistry.Binding binding = keyBindingRegistry.registerKeyBinding(id, defaultKeyCode, () -> {
        }, replayOnly);
        return new KeyBindingAdapter(binding);
    }

    @Override
    public void registerRawKey(int keyCode, ReplayKeyHandler handler) {
        keyBindingRegistry.registerRaw(keyCode, keyInput -> handler.handle(new ReplayKeyInput(keyInput.key(), keyInput.scancode(), keyInput.modifiers())));
    }

    private static class KeyBindingAdapter implements ReplayKeyBinding {
        private final ReplayKeyBindingRegistry.Binding binding;

        private KeyBindingAdapter(ReplayKeyBindingRegistry.Binding binding) {
            this.binding = binding;
        }

        @Override
        public String id() {
            return binding.name();
        }

        @Override
        public String displayName() {
            return binding.getBoundKey();
        }

        @Override
        public boolean isBound() {
            return binding.isBound();
        }

        @Override
        public boolean consumeClick() {
            if (!binding.isDown()) {
                return false;
            }
            binding.trigger();
            return true;
        }

        @Override
        public boolean isDown() {
            return binding.isDown();
        }
    }

}
