package com.replaymod.render.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.thread.ProcessorMailbox;





@Mixin(ChunkRenderDispatcher.class)
public abstract class BlockOnChunkRebuildsMixin implements ForceChunkLoadingHook.IBlockOnChunkRebuilds {
    @Shadow @Final private Queue<ChunkBufferBuilderPack> freeBuffers;
    @Unique
    private int getAvailableBufferCount() {
        return freeBuffers.size();
    }

    @Unique
    private boolean upload() {
        boolean anything = false;
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
            anything = true;
        }
        return anything;
    }

    @Shadow @Final private ProcessorMailbox<Runnable> mailbox;

    @Shadow protected abstract void runTask();

    @Shadow @Final private Queue<Runnable> toUpload;
    private final Lock waitingForWorkLock = new ReentrantLock();
    private final Condition newWork = waitingForWorkLock.newCondition();
    private volatile boolean allDone;

    private int totalBufferCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rememberTotalThreads(CallbackInfo ci) {
        this.totalBufferCount = getAvailableBufferCount();
    }

    @Inject(method = "runTask", at = @At("RETURN"))
    private void notifyMainThreadIfEverythingIsDone(CallbackInfo ci) {
        if (getAvailableBufferCount() == this.totalBufferCount) {
            // Looks like we're done, better notify the main thread in case the previous task didn't generate an upload
            this.waitingForWorkLock.lock();
            try {
                this.allDone = true;
                this.newWork.signalAll();
            } finally {
                this.waitingForWorkLock.unlock();
            }
        } else {
            this.allDone = false;
        }
    }

    @Inject(method = "uploadChunkLayer", at = @At("RETURN"))
    private void notifyMainThreadOfNewUpload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        this.waitingForWorkLock.lock();
        try {
            this.newWork.signal();
        } finally {
            this.waitingForWorkLock.unlock();
        }
    }

    private boolean waitForMainThreadWork() {
        boolean allDone = this.mailbox.<Boolean>ask(reply -> () -> {
            runTask();
            reply.tell(getAvailableBufferCount() == this.totalBufferCount);
        }).join();

        if (allDone) {
            return true;
        } else {
            this.waitingForWorkLock.lock();
            try {
                while (true) {

                    if (this.allDone) {
                        return true;
                    } else if (!this.toUpload.isEmpty()) {
                        return false;
                    } else {
                        this.newWork.awaitUninterruptibly();
                    }
                }
            } finally {
                this.waitingForWorkLock.unlock();
            }
        }
    }

    @Override
    public boolean uploadEverythingBlocking() {
        boolean anything = false;

        boolean allChunksBuilt;
        do {
            allChunksBuilt = waitForMainThreadWork();
            while (upload()) {
                anything = true;
            }
        } while (!allChunksBuilt);

        return anything;
    }
}
