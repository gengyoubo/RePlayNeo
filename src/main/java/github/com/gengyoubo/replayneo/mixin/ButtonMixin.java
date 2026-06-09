package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.feature.recording.ReplayModRecording;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.class)
public abstract class ButtonMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void replayneo$hideRecordingUiBeforeDisconnect(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof PauseScreen)) {
            return;
        }

        Button button = (Button) (Object) this;
        Component message = button.getMessage();
        if (!Component.translatable("menu.returnToMenu").equals(message)
                && !Component.translatable("menu.disconnect").equals(message)) {
            return;
        }

        ReplayModRecording.instance.getConnectionEventHandler().hideRecordingUi();
    }
}
