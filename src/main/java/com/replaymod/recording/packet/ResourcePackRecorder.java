package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket.Action;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.net.MalformedURLException;
import java.net.URL;


import de.johni0702.minecraft.gui.utils.Consumer;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Records resource packs and handles incoming resource pack packets during recording.
 */
public class ResourcePackRecorder {
    private static final Logger logger = LogManager.getLogger();
    private static final Minecraft mc = getMinecraft();

    private final ReplayFile replayFile;

    private int nextRequestId;

    public ResourcePackRecorder(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    public void recordResourcePack(Path file, int requestId) {
        try {
            // Read in resource pack file
            byte[] bytes = Files.readAllBytes(file);
            // Check whether it is already known
            String hash = Hashing.sha1().hashBytes(bytes).toString();
            boolean doWrite = false; // Whether we are the first and have to write it
            synchronized (replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index == null) {
                    index = new HashMap<>();
                }
                if (!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if (doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    public ServerboundResourcePackPacket makeStatusPacket(String hash, Action action) {
        return new ServerboundResourcePackPacket(action);
    }


    public synchronized ClientboundResourcePackPacket handleResourcePack(Connection netManager, ClientboundResourcePackPacket packet) {
        final int requestId = nextRequestId++;
        final String url = packet.getUrl();
        final String hash = packet.getHash();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.gameDirectory, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                addCallback(setServerResourcePack(levelDir), result -> {
                    recordResourcePack(levelDir.toPath(), requestId);
                    netManager.send(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED));
                }, throwable -> {
                    netManager.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
                });
            } else {
                netManager.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
            }
        } else {
            final ServerData serverData = mc.getCurrentServer();
            if (serverData != null && serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
                netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                downloadResourcePackFuture(netManager, requestId, url, hash);
            } else if (serverData != null && serverData.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT) {
                netManager.send(makeStatusPacket(hash, Action.DECLINED));
            } else {
                // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
                mc.execute(() -> mc.setScreen(new ConfirmScreen(result -> {
                        if (serverData != null) {
                            serverData.setResourcePackStatus(result ? ServerData.ServerPackStatus.ENABLED : ServerData.ServerPackStatus.DISABLED);
                        }
                        if (result) {
                            netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                            downloadResourcePackFuture(netManager, requestId, url, hash);
                        } else {
                            netManager.send(makeStatusPacket(hash, Action.DECLINED));
                        }

                        ServerList.saveSingleServer(serverData);
                        mc.setScreen(null);
                    }
                , Component.translatable("multiplayer.texturePrompt.line1"), Component.translatable("multiplayer.texturePrompt.line2"))));
            }
        }

        return new ClientboundResourcePackPacket(
                "replay://" + requestId, ""
                , packet.isRequired(), packet.getPrompt()
        );
    }

    private void downloadResourcePackFuture(Connection connection, int requestId, String url, final String hash) {
        addCallback(downloadResourcePack(requestId, url, hash),
                result -> connection.send(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED)),
                throwable -> connection.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD)));
    }

    private
    CompletableFuture<?>
    downloadResourcePack(final int requestId, String url, String hash) {
        DownloadedPackSource packFinder = mc.getDownloadedPackSource();
        ((IDownloadingPackFinder) packFinder).setRequestCallback(file -> recordResourcePack(file.toPath(), requestId));
        try {
            URL theUrl = new URL(url);
            String protocol = theUrl.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new MalformedURLException("Unsupported protocol.");
            }
            return packFinder.downloadAndSelectResourcePack(theUrl, hash, true);
        } catch (MalformedURLException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public interface IDownloadingPackFinder {
        void setRequestCallback(Consumer<File> callback);
    }

}
