package github.com.gengyoubo.replayneo.platform;

import com.replaymod.replaystudio.data.ModInfo;
import com.replaymod.replaystudio.util.I18n;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.api.ReplayRuntime;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.api.scheduler.Scheduler;
import github.com.gengyoubo.replayneo.platform.addon.ReplayModExtras;
import github.com.gengyoubo.replayneo.platform.feature.editor.ReplayModEditor;
import github.com.gengyoubo.replayneo.platform.feature.pathing.ReplayModSimplePathing;
import github.com.gengyoubo.replayneo.platform.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.gui.ReplayModGui;
import github.com.gengyoubo.replayneo.platform.restored.com.replaymod.compat.ReplayModCompat;
import github.com.gengyoubo.replayneo.platform.scheduler.SchedulerImpl;
import github.com.gengyoubo.replayneo.platform.versions.LegacyMCVer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ForgeReplayRuntime implements ReplayRuntime {
    private final SchedulerImpl scheduler = new SchedulerImpl();

    public ForgeReplayRuntime() {
        ReplayMod mod = new ReplayMod(this);
        mod.addModule(new ReplayModGui(mod));
        mod.addModule(new ReplayModRecording(mod));
        mod.addModule(new ReplayModReplay(mod));
        mod.addModule(new ReplayModRender(mod));
        mod.addModule(new ReplayModSimplePathing(mod));
        mod.addModule(new ReplayModEditor(mod));
        mod.addModule(new ReplayModExtras(mod));
        mod.addModule(new ReplayModCompat());
        mod.initModules();
    }

    @Override
    public String getVersion() {
        return ModList.get().getModContainerById(RePlayNeo.MODID)
                .orElseThrow(IllegalStateException::new)
                .getModInfo().getVersion().toString();
    }

    @Override
    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id.toLowerCase());
    }

    @Override
    public Path getGameDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    @Override
    public Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModInfo> modInfoMap = ModList.get().getMods().stream()
                .map(m -> new ModInfo(m.getModId(), m.getDisplayName(), m.getVersion().toString()))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));
        return BuiltInRegistries.REGISTRY.stream()
                .map(Registry::keySet).flatMap(Set::stream)
                .map(ResourceLocation::getNamespace).filter(s -> !s.equals("minecraft")).distinct()
                .map(modInfoMap::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public ReplayKeyBindingRegistry keyBindingRegistry() {
        return ReplayPlatforms.get().input().keyBindingRegistry();
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public int getProtocolVersion() {
        return LegacyMCVer.getProtocolVersion();
    }

    @Override
    public void configureI18n() {
        I18n.setI18n(net.minecraft.client.resources.language.I18n::get);
    }

    @Override
    public void sendReplayMessage(boolean warning, String message, Object... args) {
        ReplayPlatforms.get().client().sendReplayMessage(warning, message, args);
    }

    @Override
    public void executeOnClient(Runnable runnable) {
        ReplayPlatforms.get().client().execute(runnable);
    }
}
