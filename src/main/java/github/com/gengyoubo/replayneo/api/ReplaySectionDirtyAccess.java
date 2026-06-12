package github.com.gengyoubo.replayneo.api;

public interface ReplaySectionDirtyAccess {
    void replayneo$markSectionDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread);
}
