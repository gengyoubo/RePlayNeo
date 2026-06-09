package github.com.gengyoubo.replayneo.platform;

import com.replaymod.replaystudio.data.ModInfo;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.ReplayRuntime;
import github.com.gengyoubo.replayneo.core.ReplayMod;
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
    public ForgeReplayRuntime() {
        ReplayMod mod = new ReplayMod(this);
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
}
