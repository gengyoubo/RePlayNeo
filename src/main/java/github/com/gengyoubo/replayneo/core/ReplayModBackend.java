package github.com.gengyoubo.replayneo.core;

import github.com.gengyoubo.replayneo.RePlayNeo;
import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;

public class ReplayModBackend {

    public ReplayModBackend() {
        ReplayMod mod = new ReplayMod(this);
        mod.initModules();
    }

    public String getVersion() {
        return ModList.get().getModContainerById(RePlayNeo.MODID)
                .orElseThrow(IllegalStateException::new)
                .getModInfo().getVersion().toString();
    }

    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id.toLowerCase());
    }
}
