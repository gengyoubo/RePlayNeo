package github.com.gengyoubo.replayneo.core;

import github.com.gengyoubo.replayneo.restored.com.replaymod.compat.ReplayModCompat;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.files.ReplayFilesService;
import github.com.gengyoubo.replayneo.core.files.ReplayFoldersService;
import github.com.gengyoubo.replayneo.core.gui.GuiBackgroundProcesses;
import github.com.gengyoubo.replayneo.core.gui.GuiReplaySettings;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.scheduler.Scheduler;
import github.com.gengyoubo.replayneo.platform.scheduler.SchedulerImpl;
import github.com.gengyoubo.replayneo.feature.editor.ReplayModEditor;
import github.com.gengyoubo.replayneo.addon.ReplayModExtras;
import github.com.gengyoubo.replayneo.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.feature.render.ReplayModRender;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.util.I18n;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.feature.pathing.ReplayModSimplePathing;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PathPackResources;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;



public class ReplayMod implements Module, Scheduler {

    public static String MOD_ID = RePlayNeo.MODID;

    public static ResourceLocation TEXTURE = identifier(RePlayNeo.RESOURCE_NAMESPACE, "replay_gui.png");
    public static int TEXTURE_SIZE = 256;
    public static ResourceLocation LOGO_FAVICON = identifier(RePlayNeo.RESOURCE_NAMESPACE, "favicon_logo.png");

    private static final Minecraft mc = MCVer.getMinecraft();

    private final ReplayModBackend backend;
    private final SchedulerImpl scheduler = new SchedulerImpl();
    private final ReplayKeyBindingRegistry keyBindingRegistry = ReplayPlatforms.get().input().keyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    {
        settingsRegistry.register(Setting.class);
    }

    { instance = this; }
    public static ReplayMod instance;

    private final List<Module> modules = new ArrayList<>();

    private final GuiBackgroundProcesses backgroundProcesses = new GuiBackgroundProcesses();
    public final ReplayFoldersService folders = new ReplayFoldersService(settingsRegistry);
    public final ReplayFilesService files = new ReplayFilesService(folders);

    /**
     * Whether the current MC version is supported by the embedded ReplayStudio version.
     * If this is not the case (i.e. if this is variable true), any feature of the RM which depends on the ReplayStudio
     * lib will be disabled.
     * <p>
     * Only supported on Fabric builds, i.e. will always be false / crash the game with Forge/pre-1.14 builds.
     * (specifically the code below and MCVer#getProtocolVersion make this assumption)
     */
    private boolean minimalMode;

    public ReplayMod(ReplayModBackend backend) {
        this.backend = backend;

        I18n.setI18n(net.minecraft.client.resources.language.I18n::get);

        // Check Minecraft protocol version for compatibility
        if (!ProtocolVersion.isRegistered(MCVer.getProtocolVersion()) && !Boolean.parseBoolean(System.getProperty("replaymod.skipversioncheck", "false"))) {
            minimalMode = true;
        }

        // Register all RM modules
        modules.add(this);
        modules.add(new ReplayModRecording(this));
        ReplayModReplay replayModule = new ReplayModReplay(this);
        modules.add(replayModule);
        modules.add(new ReplayModRender(this));
        modules.add(new ReplayModSimplePathing(this));
        modules.add(new ReplayModEditor(this));
        modules.add(new ReplayModExtras(this));
        modules.add(new ReplayModCompat());

        settingsRegistry.register();
    }

    public ReplayKeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public static PathPackResources jGuiResourcePack = createJGuiResourcePack();
    public static String JGUI_RESOURCE_PACK_NAME = "replaymod_jgui";
    private static PathPackResources createJGuiResourcePack() {
        File folder = new File("../jGui/src/main/resources");
        if (!folder.exists()) {
            folder = new File("../../../jGui/src/main/resources");
            if (!folder.exists()) {
                return null;
            }
        }
        if (JGUI_RESOURCE_PACK_NAME != null) {
            return new PathPackResources(JGUI_RESOURCE_PACK_NAME, folder.toPath(), true) {
                @Override
                public @NotNull String packId() {
                    return JGUI_RESOURCE_PACK_NAME;
                }

                @Override
                public net.minecraft.server.packs.resources.IoSupplier<InputStream> getRootResource(String @NotNull ... segments) {
                    if (segments.length == 1 && segments[0].equals("pack.mcmeta")) {
                        return () -> new ByteArrayInputStream(generatePackMeta());
                    }
                    return super.getRootResource(segments);
                }

                private byte[] generatePackMeta() {
                    int version = 4;
                    return ("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": "
                            + version + "}}").getBytes(StandardCharsets.UTF_8);
                }
            };
        }
        return null;
    }

    void initModules() {
        modules.forEach(Module::initCommon);
        modules.forEach(Module::initClient);
        modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    }

    @Override
    public void registerKeyBindings(ReplayKeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.settings", 0, () -> new GuiReplaySettings(null, settingsRegistry).display(), false);
    }

    @Override
    public void initClient() {
        backgroundProcesses.register();
        keyBindingRegistry.register();

        // 1.7.10 crashes when render distance > 16
        // Post 1.19 this has become non-trivial to do, install Sodium+Bobby or OptiFine if you need it

        runPostStartup(() -> files.initialScan(this));
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

    public Minecraft getMinecraft() {
        return mc;
    }

    public void printInfoToChat(String message, Object... args) {
        printToChat(false, message, args);
    }

    public void printWarningToChat(String message, Object... args) {
        printToChat(true, message, args);
    }

    private void printToChat(boolean warning, String message, Object... args) {
        if (!mc.isSameThread()) {
            runLater(() -> printToChat(warning, message, args));
            return;
        }
        if (getSettingsRegistry().get(Setting.NOTIFICATIONS)) {
            // Some nostalgia: "§8[§6Replay Mod§8]§r Your message goes here"
            Style coloredDarkGray = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            Style coloredGold = Style.EMPTY.withColor(ChatFormatting.GOLD);
            Style alert = Style.EMPTY.withColor(warning ? ChatFormatting.RED : ChatFormatting.DARK_GREEN);
            Component text = Component.literal("[").setStyle(coloredDarkGray)
                    .append(Component.translatable("replaymod.title").setStyle(coloredGold))
                    .append(Component.literal("] "))
                    .append(Component.translatable(message, args).setStyle(alert));
            // Send message to chat GUI
            // The ingame GUI is initialized at startup, therefore this is possible before the client is connected
            mc.gui.getChat().addMessage(text);
        }
    }

    public GuiBackgroundProcesses getBackgroundProcesses() {
        return backgroundProcesses;
    }

    // This method is static because it depends solely on the environment, not on the actual RM instance.
    public static boolean isMinimalMode() {
        return ReplayMod.instance.minimalMode;
    }

    public static boolean isCompatible(int fileFormatVersion, int protocolVersion) {
        if (isMinimalMode()) {
            return protocolVersion == MCVer.getProtocolVersion();
        } else {
            return new ReplayStudio().isCompatible(fileFormatVersion, protocolVersion, MCVer.getProtocolVersion());
        }
    }
}
