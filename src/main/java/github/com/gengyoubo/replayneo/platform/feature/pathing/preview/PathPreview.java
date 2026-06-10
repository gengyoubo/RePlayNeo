package github.com.gengyoubo.replayneo.platform.feature.pathing.preview;

import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.api.events.SettingsChangedCallback;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.render.events.ReplayClosedCallback;
import github.com.gengyoubo.replayneo.platform.render.events.ReplayOpenedCallback;
import github.com.gengyoubo.replayneo.platform.feature.pathing.ReplayModSimplePathing;
import github.com.gengyoubo.replayneo.platform.feature.pathing.Setting;

public class PathPreview extends EventRegistrations {
    private final ReplayModSimplePathing mod;

    private ReplayHandler replayHandler;
    private PathPreviewRenderer renderer;

    public PathPreview(ReplayModSimplePathing mod) {
        this.mod = mod;

        on(SettingsChangedCallback.EVENT, (registry, key) -> {
            if (key == Setting.PATH_PREVIEW) {
                update();
            }
        });

        on(ReplayOpenedCallback.EVENT, replayHandler -> {
            this.replayHandler = replayHandler;
            update();
        });

        on(ReplayClosedCallback.EVENT, replayHandler -> {
            this.replayHandler = null;
            update();
        });
    }

    public void registerKeyBindings(ReplayKeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.pathpreview", Keyboard.KEY_H, () -> {
            SettingsRegistry settings = mod.getCore().getSettingsRegistry();
            settings.set(Setting.PATH_PREVIEW, !settings.get(Setting.PATH_PREVIEW));
            settings.save();
        }, true);
    }

    private void update() {
        if (mod.getCore().getSettingsRegistry().get(Setting.PATH_PREVIEW) && replayHandler != null) {
            if (renderer == null) {
                renderer = new PathPreviewRenderer(mod, replayHandler);
                renderer.register();
            }
        } else {
            if (renderer != null) {
                renderer.unregister();
                renderer = null;
            }
        }
    }
}
