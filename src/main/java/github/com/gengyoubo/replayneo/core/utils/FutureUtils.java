package github.com.gengyoubo.replayneo.core.utils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.replaystudio.lib.guava.util.concurrent.Uninterruptibles;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public final class FutureUtils {
    private FutureUtils() {}

    public static <V> void addCallback(
            ListenableFuture<V> future,
            FutureCallback<? super V> callback,
            Executor executor) {

        Objects.requireNonNull(future);
        Objects.requireNonNull(callback);
        Objects.requireNonNull(executor);

        future.addListener(() -> {
            try {
                callback.onSuccess(
                        Uninterruptibles.getUninterruptibly(future)
                );

            } catch (RuntimeException | Error e) {
                callback.onFailure(e);
            } catch (ExecutionException e) {
                callback.onFailure(e.getCause());

            }

        }, executor);
    }

    public static <V> void addCallback(
            ListenableFuture<V> future,
            FutureCallback<? super V> callback) {

        addCallback(
                future,
                callback,
                Runnable::run
        );
    }
}
