package github.com.gengyoubo.replayneo.platform.feature.render.rendering;

import github.com.gengyoubo.replayneo.api.render.RenderSettings;
import github.com.gengyoubo.replayneo.platform.feature.render.blend.BlendFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.CubicOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.CubicPboOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.ODSFrameCapturer;
import github.com.gengyoubo.replayneo.api.render.capturer.RenderInfo;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.SimpleOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.SimplePboOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.StereoscopicOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.platform.feature.render.capturer.StereoscopicPboOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.api.render.capturer.WorldRenderer;
import github.com.gengyoubo.replayneo.core.render.frame.CubicOpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.ODSOpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.render.frame.StereoscopicOpenGlFrame;
import github.com.gengyoubo.replayneo.platform.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.core.render.processor.CubicToBitmapProcessor;
import github.com.gengyoubo.replayneo.core.render.processor.DummyProcessor;
import github.com.gengyoubo.replayneo.core.render.processor.EquirectangularToBitmapProcessor;
import github.com.gengyoubo.replayneo.core.render.processor.ODSToBitmapProcessor;
import github.com.gengyoubo.replayneo.core.render.processor.OpenGlToBitmapProcessor;
import github.com.gengyoubo.replayneo.core.render.processor.StereoscopicToBitmapProcessor;
import github.com.gengyoubo.replayneo.core.render.rendering.Channel;
import github.com.gengyoubo.replayneo.api.frame.FrameCapturer;
import github.com.gengyoubo.replayneo.api.frame.FrameConsumer;
import github.com.gengyoubo.replayneo.platform.feature.render.PixelBufferObject;

import java.util.Map;

public class Pipelines {
    public static Pipeline newPipeline(RenderSettings.RenderMethod method, RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        return switch (method) {
            case DEFAULT -> newDefaultPipeline(renderInfo, consumer);
            case STEREOSCOPIC -> newStereoscopicPipeline(renderInfo, consumer);
            case CUBIC -> newCubicPipeline(renderInfo, consumer);
            case EQUIRECTANGULAR -> newEquirectangularPipeline(renderInfo, consumer);
            case ODS -> newODSPipeline(renderInfo, consumer);
            case BLEND -> throw new UnsupportedOperationException("Use newBlendPipeline instead!");
        };
    }

    public static Pipeline<OpenGlFrame, BitmapFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<OpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new SimplePboOpenGlFrameCapturer(worldRenderer, renderInfo);
        } else {
            capturer = new SimpleOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        return new Pipeline<>(worldRenderer, capturer, new OpenGlToBitmapProcessor(), consumer);
    }

    public static Pipeline<StereoscopicOpenGlFrame, BitmapFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<StereoscopicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new StereoscopicPboOpenGlFrameCapturer(worldRenderer, renderInfo);
        } else {
            capturer = new StereoscopicOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        return new Pipeline<>(worldRenderer, capturer, new StereoscopicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, BitmapFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        }
        return new Pipeline<>(worldRenderer, capturer, new CubicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, BitmapFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        EquirectangularToBitmapProcessor processor = new EquirectangularToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        }
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<ODSOpenGlFrame, BitmapFrame> newODSPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        ODSToBitmapProcessor processor = new ODSToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        boolean iris = net.minecraftforge.fml.ModList.get().isLoaded("iris");
        FrameCapturer<ODSOpenGlFrame> capturer = iris
                ? new github.com.gengyoubo.replayneo.platform.feature.render.capturer.IrisODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize())
                : new ODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<BitmapFrame, BitmapFrame> newBlendPipeline(RenderInfo renderInfo) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<BitmapFrame> capturer = new BlendFrameCapturer(worldRenderer, renderInfo);
        FrameConsumer<BitmapFrame> consumer = new FrameConsumer<>() {
            @Override
            public void consume(Map<Channel, BitmapFrame> channels) {
            }

            @Override
            public void close() {
            }

            @Override
            public boolean isParallelCapable() {
                return true;
            }
        };
        return new Pipeline<>(worldRenderer, capturer, new DummyProcessor<>(), consumer);
    }
}

