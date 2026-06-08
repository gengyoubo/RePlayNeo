package github.com.gengyoubo.replayneo.core;

public class ExtrasInitMixin {
    public void onPreLaunch() {
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
    }
}
