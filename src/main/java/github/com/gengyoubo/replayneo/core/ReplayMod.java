package github.com.gengyoubo.replayneo.core;

import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.studio.ReplayStudio;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.Module;
import github.com.gengyoubo.replayneo.api.ReplayRuntime;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.files.ReplayFilesService;
import github.com.gengyoubo.replayneo.core.files.ReplayFoldersService;
import github.com.gengyoubo.replayneo.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ReplayMod implements Module, Scheduler {
    public static String MOD_ID = RePlayNeo.MODID;

    public static ReplayMod instance;

    private final ReplayRuntime backend;
    private final Scheduler scheduler;
    private final ReplayKeyBindingRegistry keyBindingRegistry;
    private final SettingsRegistry settingsRegistry;
    private final int protocolVersion;
    private final List<Module> modules = new ArrayList<>();

    public final ReplayFoldersService folders;
    public final ReplayFilesService files;

    /**
     * Whether the current MC version is supported by the embedded ReplayStudio version.
     * If this is not the case, ReplayStudio-backed features will be disabled.
     */
    private boolean minimalMode;

    public ReplayMod(ReplayRuntime backend) {
        instance = this;
        this.backend = backend;
        this.scheduler = backend.scheduler();
        this.keyBindingRegistry = backend.keyBindingRegistry();
        this.protocolVersion = backend.getProtocolVersion();
        this.settingsRegistry = new SettingsRegistry(backend.getGameDirectory(), backend::executeOnClient);
        this.settingsRegistry.register(Setting.class);
        this.folders = new ReplayFoldersService(backend.getGameDirectory(), settingsRegistry);
        this.files = new ReplayFilesService(folders);

        backend.configureI18n();

        if (!ProtocolVersion.isRegistered(protocolVersion)
                && !Boolean.parseBoolean(System.getProperty("replaymod.skipversioncheck", "false"))) {
            minimalMode = true;
        }

        modules.add(this);
        settingsRegistry.register();
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    public ReplayKeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public void initModules() {
        modules.forEach(Module::initCommon);
        modules.forEach(Module::initClient);
        modules.forEach(module -> module.registerKeyBindings(keyBindingRegistry));
    }

    @Override
    public void initClient() {
        keyBindingRegistry.register();
    }

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        scheduler.runSync(runnable);
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        scheduler.runPostStartup(runnable);
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        scheduler.runLaterWithoutLock(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        scheduler.runLater(runnable);
    }

    @Override
    public void runTasks() {
        scheduler.runTasks();
    }

    public String getVersion() {
        return backend.getVersion();
    }

    public String getMinecraftVersion() {
        return backend.getMinecraftVersion();
    }

    public boolean isModLoaded(String id) {
        return backend.isModLoaded(id);
    }

    public Collection<com.replaymod.replaystudio.data.ModInfo> getInstalledNetworkMods() {
        return backend.getInstalledNetworkMods();
    }

    public void printInfoToChat(String message, Object... args) {
        printToChat(false, message, args);
    }

    public void printWarningToChat(String message, Object... args) {
        printToChat(true, message, args);
    }

    private void printToChat(boolean warning, String message, Object... args) {
        if (getSettingsRegistry().get(Setting.NOTIFICATIONS)) {
            backend.sendReplayMessage(warning, message, args);
        }
    }

    public static boolean isMinimalMode() {
        return ReplayMod.instance.minimalMode;
    }

    public static boolean isCompatible(int fileFormatVersion, int protocolVersion) {
        int currentProtocolVersion = ReplayMod.instance.protocolVersion;
        if (isMinimalMode()) {
            return protocolVersion == currentProtocolVersion;
        }
        return new ReplayStudio().isCompatible(fileFormatVersion, protocolVersion, currentProtocolVersion);
    }
}
