package github.com.gengyoubo.replayneo.api.network;

public interface ReplayPacket {
    int protocolId();

    String typeName();

    byte[] payload();
}
