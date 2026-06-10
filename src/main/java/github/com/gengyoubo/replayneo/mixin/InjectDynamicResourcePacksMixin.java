package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.versions.JGuiResourcePack;
import github.com.gengyoubo.replayneo.platform.versions.LangResourcePack;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BiConsumer;
import java.util.stream.Collector;


@Mixin(net.minecraft.server.packs.repository.PackRepository.class)
public class InjectDynamicResourcePacksMixin {
    @ModifyArg(
            method = "openAllSelected",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;")
    )
    private Collector<PackResources, ?, ?> injectReplayModPacks(Collector<PackResources, ?, ?> collector) {
        collector = rePlay$append(collector, new LangResourcePack());
        PackResources jGuiResourcePack = JGuiResourcePack.get();
        if (jGuiResourcePack != null) {
            collector = rePlay$append(collector, jGuiResourcePack);
        }
        return collector;
    }

    @Unique
    private static <T, A, R> Collector<T, A, R> rePlay$append(Collector<T, A, R> collector, T value) {
        BiConsumer<A, T> accumulator = collector.accumulator();
        return Collector.of(
                collector.supplier(),
                accumulator,
                collector.combiner(),
                result -> {
                    accumulator.accept(result, value);
                    return collector.finisher().apply(result);
                }
        );
    }
}
