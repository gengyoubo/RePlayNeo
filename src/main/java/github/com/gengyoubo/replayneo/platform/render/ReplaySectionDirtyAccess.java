package github.com.gengyoubo.replayneo.platform.render;

public interface ReplaySectionDirtyAccess {
    void replayneo$markSectionDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread);
}
