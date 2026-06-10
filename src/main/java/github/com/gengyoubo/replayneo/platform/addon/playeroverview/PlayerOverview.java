package github.com.gengyoubo.replayneo.platform.addon.playeroverview;

import github.com.gengyoubo.replayneo.platform.gui.GuiUtils;

import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.api.events.PreRenderHandCallback;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.platform.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.api.Extra;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.render.events.ReplayClosedCallback;
import github.com.gengyoubo.replayneo.platform.render.events.ReplayOpenedCallback;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import com.replaymod.replaystudio.lib.guava.base.Optional;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import java.util.stream.Collectors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.io.IOException;
import java.util.*;

public class PlayerOverview extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private boolean savingEnabled;

    @Override
    public void register(final RePlayCore mod) throws Exception {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.playeroverview", Keyboard.KEY_B, () -> {
            if (module.getReplayHandler() != null) {
                List<Player> players = null;
                if (MCVer.getMinecraft().level != null) {
                    players = MCVer.getMinecraft().level.players()
                            .stream()
                            .map(it -> (Player) it)
                            .filter(it -> !(it instanceof CameraEntity))
                            .collect(Collectors.toList());
                }
                if (!GuiUtils.isCtrlDown()) {
                    // Hide all players that have an UUID v2 (commonly used for NPCs)
                    Iterator<Player> iter = players.iterator();
                    while (iter.hasNext()) {
                        UUID uuid = iter.next().getGameProfile().getId();
                        if (uuid != null && uuid.version() == 2) {
                            iter.remove();
                        }
                    }
                }
                new PlayerOverviewGui(PlayerOverview.this, players).display();
            }
        }, true);

        register();
    }

    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        if (hidden) {
            hiddenPlayers.add(uuid);
        } else {
            hiddenPlayers.remove(uuid);
        }
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpen); }
    private void onReplayOpen(ReplayHandler replayHandler) throws IOException {
        Optional<Set<UUID>> savedData = replayHandler.getReplayFile().getInvisiblePlayers();
        if (savedData.isPresent()) {
            hiddenPlayers.addAll(savedData.get());
            savingEnabled = true;
        } else {
            savingEnabled = false;
        }
    }

    { on(ReplayClosedCallback.EVENT, this::onReplayClose); }
    private void onReplayClose(ReplayHandler replayHandler) {
        hiddenPlayers.clear();
    }

    { on(PreRenderHandCallback.EVENT, this::shouldHideHand); }
    private boolean shouldHideHand() {
        Entity view = MCVer.getMinecraft().getCameraEntity();
        return view != null && isHidden(view.getUUID());
    }

    // See EntityRendererMixin for why this is 1.7.10 only

    public boolean isSavingEnabled() {
        return savingEnabled;
    }

    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    public void saveHiddenPlayers() {
        if (savingEnabled) {
            try {
                module.getReplayHandler().getReplayFile().writeInvisiblePlayers(hiddenPlayers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
