package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.feature.render.hooks.ForceChunkLoadingHook;
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
    private int rePlay$getAvailableBufferCount() {
        return freeBuffers.size();
    }

    @Unique
    private boolean rePlay$upload() {
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
    @Unique
    private Lock rePlay$waitingForWorkLock;
    @Unique
    private Condition rePlay$newWork;
    @Unique
    private volatile boolean rePlay$allDone;

    @Unique
    private int rePlay$totalBufferCount;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void rememberTotalThreads(CallbackInfo ci) {
        rePlay$ensureWorkSignal();
        this.rePlay$totalBufferCount = rePlay$getAvailableBufferCount();
    }

    @Inject(method = "runTask", at = @At("RETURN"))
    private void notifyMainThreadIfEverythingIsDone(CallbackInfo ci) {
        if (rePlay$getAvailableBufferCount() == this.rePlay$totalBufferCount) {
            // Looks like we're done, better notify the main thread in case the previous task didn't generate an upload
            rePlay$ensureWorkSignal();
            this.rePlay$waitingForWorkLock.lock();
            try {
                this.rePlay$allDone = true;
                this.rePlay$newWork.signalAll();
            } finally {
                this.rePlay$waitingForWorkLock.unlock();
            }
        } else {
            this.rePlay$allDone = false;
        }
    }

    @Inject(method = "uploadChunkLayer", at = @At("RETURN"))
    private void notifyMainThreadOfNewUpload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        rePlay$ensureWorkSignal();
        this.rePlay$waitingForWorkLock.lock();
        try {
            this.rePlay$newWork.signal();
        } finally {
            this.rePlay$waitingForWorkLock.unlock();
        }
    }

    @Unique
    private boolean rePlay$waitForMainThreadWork() {
        boolean allDone = this.mailbox.<Boolean>ask(reply -> () -> {
            runTask();
            reply.tell(rePlay$getAvailableBufferCount() == this.rePlay$totalBufferCount);
        }).join();

        if (allDone) {
            return true;
        } else {
            rePlay$ensureWorkSignal();
            this.rePlay$waitingForWorkLock.lock();
            try {
                while (true) {

                    if (this.rePlay$allDone) {
                        return true;
                    } else if (!this.toUpload.isEmpty()) {
                        return false;
                    } else {
                        this.rePlay$newWork.awaitUninterruptibly();
                    }
                }
            } finally {
                this.rePlay$waitingForWorkLock.unlock();
            }
        }
    }

    @Unique
    private void rePlay$ensureWorkSignal() {
        if (this.rePlay$waitingForWorkLock == null) {
            this.rePlay$waitingForWorkLock = new ReentrantLock();
            this.rePlay$newWork = this.rePlay$waitingForWorkLock.newCondition();
        }
    }

    @Override
    public boolean uploadEverythingBlocking() {
        boolean anything = false;

        boolean allChunksBuilt;
        do {
            allChunksBuilt = rePlay$waitForMainThreadWork();
            while (rePlay$upload()) {
                anything = true;
            }
        } while (!allChunksBuilt);

        return anything;
    }
}
