package github.com.gengyoubo.replayneo.api.network;

@FunctionalInterface
public interface ReplayPacketListener {
    void onPacket(ReplayPacket packet);
}
