package com.replaymod.core;

public class MixinExtrasInit {
    public void onPreLaunch() {
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
    }
}
