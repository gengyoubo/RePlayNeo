package github.com.gengyoubo.replayneo.platform.feature.recording;

import net.minecraft.client.multiplayer.ServerData;

/** Extension interface for {@link net.minecraft.client.multiplayer.ServerData}. */
public interface ServerInfoExt {

    static ServerInfoExt from(ServerData base) {
        return (ServerInfoExt) base;
    }

    /** Per-server optional overwrite for automatic recording. */
    default Boolean getAutoRecording() {
        return null;
    }

    /** Per-server optional overwrite for automatic recording. */
    default void setAutoRecording(Boolean autoRecording) {
    }
}
