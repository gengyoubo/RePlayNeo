package github.com.gengyoubo.replayneo.api.network;

public interface ReplayNetwork {
    void send(ReplayPacket packet);

    void addListener(ReplayPacketListener listener);
}
