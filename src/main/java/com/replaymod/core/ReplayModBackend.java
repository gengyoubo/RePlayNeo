package com.replaymod.core;

import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import static com.replaymod.core.ReplayMod.MOD_ID;

@Mod(MOD_ID)
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);

    public ReplayModBackend() {
        mod.initModules();
    }

    public String getVersion() {
        return ModList.get().getModContainerById(MOD_ID)
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
