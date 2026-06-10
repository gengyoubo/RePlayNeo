package github.com.gengyoubo.replayneo.api.render;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import github.com.gengyoubo.replayneo.api.util.FileTypeAdapter;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class RenderSettings {
    private static volatile Function<String, String> translator = Function.identity();
    private static volatile Supplier<String> ffmpegFinder = () -> "ffmpeg";

    public static void setTranslator(Function<String, String> translator) {
        RenderSettings.translator = Objects.requireNonNull(translator);
    }

    public static void setFfmpegFinder(Supplier<String> ffmpegFinder) {
        RenderSettings.ffmpegFinder = Objects.requireNonNull(ffmpegFinder);
    }

    private static String translate(String key) {
        return translator.apply(key);
    }

    public enum RenderMethod {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR, ODS, BLEND;

        @Override
        public String toString() {
            return translate("replaymod.gui.rendersettings.renderer." + name().toLowerCase());
        }

        public String getDescription() {
            return translate("replaymod.gui.rendersettings.renderer." + name().toLowerCase() + ".description");
        }

        public boolean isSpherical() {
            return this == EQUIRECTANGULAR || this == ODS;
        }

        public boolean hasFixedAspectRatio() {
            return this == EQUIRECTANGULAR || this == ODS || this == CUBIC;
        }

        public boolean isSupported() {
            return true;
        }

        public static RenderMethod[] getSupported() {
            return Arrays.stream(values()).filter(RenderMethod::isSupported).toArray(RenderMethod[]::new);
        }
    }

    public enum EncodingPreset {
        MP4_CUSTOM("-an -c:v libx264 -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        MP4_POTATO("-an -c:v libx264 -preset ultrafast -crf 51 -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        WEBM_CUSTOM("-an -c:v libvpx -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"", "webm"),

        MKV_LOSSLESS("-an -c:v libx264 -preset ultrafast -qp 0 \"%FILENAME%\"", "mkv"),

        BLEND(null, "blend"),

        EXR(null, "exr"),

        PNG(null, "png");

        private final String preset;
        private final String fileExtension;

        EncodingPreset(String preset, String fileExtension) {
            this.preset = preset;
            this.fileExtension = fileExtension;
        }

        public String getValue() {
            return "-y -f rawvideo -pix_fmt bgra -s %WIDTH%x%HEIGHT% -r %FPS% -i - %FILTERS%"
                    + (preset == null ? "" : preset);
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public boolean hasBitrateSetting() {
            return preset != null && preset.contains("%BITRATE%");
        }

        public boolean isYuv420() { return preset != null && preset.contains("-pix_fmt yuv420p"); }

        @Override
        public String toString() {
            return translate("replaymod.gui.rendersettings.presets." + name().replace('_', '.').toLowerCase());
        }

        public boolean isSupported() {
            if (this == BLEND) {
                return RenderMethod.BLEND.isSupported();
            } else {
                return true;
            }
        }

        public static EncodingPreset[] getSupported() {
            return Arrays.stream(values()).filter(EncodingPreset::isSupported).toArray(EncodingPreset[]::new);
        }
    }

    public enum AntiAliasing {
        NONE(1), X2(2), X4(4), X8(8);

        private final int factor;

        AntiAliasing(int factor) {
            this.factor = factor;
        }

        public int getFactor() {
            return factor;
        }

        @Override
        public String toString() {
            return translate("replaymod.gui.rendersettings.antialiasing." + name().toLowerCase());
        }
    }

    private final RenderMethod renderMethod;
    private final EncodingPreset encodingPreset;
    private final int videoWidth;
    private final int videoHeight;
    private final int framesPerSecond;
    private final int bitRate;
    @JsonAdapter(FileTypeAdapter.class)
    private final File outputFile;

    private final boolean renderNameTags;
    private final boolean includeAlphaChannel;
    private final boolean stabilizeYaw;
    private final boolean stabilizePitch;
    private final boolean stabilizeRoll;
    private final Color chromaKeyingColor;
    private final int sphericalFovX;
    private final int sphericalFovY;
    private final boolean injectSphericalMetadata;
    private final boolean depthMap;
    private final boolean cameraPathExport;
    private final AntiAliasing antiAliasing;

    private final String exportCommand;
    // We switched from rgb24 to bgra for performance at one point, so for backwards compatibility we need to
    // reset the arguments if they're from an older version. Easiest way to do that is to just change the key
    // and handle the null during loading.
    @SerializedName("exportArguments")
    // using an empty string as default because old versions will realize it's not a preset and prompt the user
    private final String exportArgumentsPreBgra = "";
    @SerializedName("exportArgumentsBgra")
    private final String exportArguments;

    private final boolean highPerformance;

    public RenderSettings() {
        this(
                RenderSettings.RenderMethod.DEFAULT,
                RenderSettings.EncodingPreset.MP4_CUSTOM,
                1920,
                1080,
                60,
                20 << 20,
                null,
                true,
                false,
                false,
                false,
                false,
                null,
                360,
                180,
                false,
                false,
                false,
                RenderSettings.AntiAliasing.NONE,
                "",
                RenderSettings.EncodingPreset.MP4_CUSTOM.getValue(),
                false
        );
    }

    public RenderSettings(
            RenderMethod renderMethod,
            EncodingPreset encodingPreset,
            int videoWidth,
            int videoHeight,
            int framesPerSecond,
            int bitRate,
            File outputFile,
            boolean renderNameTags,
            boolean includeAlphaChannel,
            boolean stabilizeYaw,
            boolean stabilizePitch,
            boolean stabilizeRoll,
            ReadableColor chromaKeyingColor,
            int sphericalFovX,
            int sphericalFovY,
            boolean injectSphericalMetadata,
            boolean depthMap,
            boolean cameraPathExport,
            AntiAliasing antiAliasing,
            String exportCommand,
            String exportArguments,
            boolean highPerformance
    ) {
        this.renderMethod = renderMethod;
        this.encodingPreset = encodingPreset;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.framesPerSecond = framesPerSecond;
        this.bitRate = bitRate;
        this.outputFile = outputFile;
        this.renderNameTags = renderNameTags;
        this.includeAlphaChannel = includeAlphaChannel;
        this.stabilizeYaw = stabilizeYaw;
        this.stabilizePitch = stabilizePitch;
        this.stabilizeRoll = stabilizeRoll;
        this.chromaKeyingColor = chromaKeyingColor == null ? null : new Color(chromaKeyingColor);
        this.sphericalFovX = sphericalFovX;
        this.sphericalFovY = sphericalFovY;
        this.injectSphericalMetadata = injectSphericalMetadata;
        this.depthMap = depthMap;
        this.cameraPathExport = cameraPathExport;
        this.antiAliasing = antiAliasing;
        this.exportCommand = exportCommand;
        this.exportArguments = exportArguments;
        this.highPerformance = highPerformance;
    }

    public RenderSettings withEncodingPreset(EncodingPreset encodingPreset) {
        return new RenderSettings(
                renderMethod,
                encodingPreset,
                videoWidth,
                videoHeight,
                framesPerSecond,
                bitRate,
                outputFile,
                renderNameTags,
                includeAlphaChannel,
                stabilizeYaw,
                stabilizePitch,
                stabilizeRoll,
                chromaKeyingColor,
                sphericalFovX,
                sphericalFovY,
                injectSphericalMetadata,
                depthMap,
                cameraPathExport,
                antiAliasing,
                exportCommand,
                exportArguments,
                highPerformance
        );
    }

    /**
     * @return the width of the output video during rendering, including the upscale for Anti-Aliasing.
     */
    public int getVideoWidth() {
        return videoWidth * antiAliasing.getFactor();
    }

    /**
     * @return the height of the output video during rendering, including the upscale for Anti-Aliasing.
     */
    public int getVideoHeight() {
        return videoHeight * antiAliasing.getFactor();
    }

    /**
     * @return the actual width of the output video.
     */
    public int getTargetVideoWidth() {
        return videoWidth;
    }

    /**
     * @return the actual height of the output video.
     */
    public int getTargetVideoHeight() {
        return videoHeight;
    }

    public String getVideoFilters() {
        StringBuilder filters = new StringBuilder();

        if (antiAliasing != AntiAliasing.NONE) {
            double factor = 1.0 / antiAliasing.getFactor();
            filters.append(String.format("-filter:v scale=iw*%1$s:ih*%1$s ", factor));
        }

        return filters.toString();
    }

    public String getExportCommandOrDefault() {
        return exportCommand.isEmpty() ? ffmpegFinder.get() : exportCommand;
    }

    public RenderMethod getRenderMethod() {
        return renderMethod;
    }

    public EncodingPreset getEncodingPreset() {
        return encodingPreset;
    }

    public boolean requiresFFmpeg() {
        return renderMethod != RenderMethod.BLEND
                && encodingPreset != EncodingPreset.EXR
                && encodingPreset != EncodingPreset.PNG;
    }

    public int getFramesPerSecond() {
        return framesPerSecond;
    }

    public int getBitRate() {
        return bitRate;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean isRenderNameTags() {
        return renderNameTags;
    }

    public boolean isIncludeAlphaChannel() {
        return includeAlphaChannel;
    }

    public boolean isStabilizeYaw() {
        return stabilizeYaw;
    }

    public boolean isStabilizePitch() {
        return stabilizePitch;
    }

    public boolean isStabilizeRoll() {
        return stabilizeRoll;
    }

    public ReadableColor getChromaKeyingColor() {
        return chromaKeyingColor;
    }

    public int getSphericalFovX() {
        return sphericalFovX;
    }

    public int getSphericalFovY() {
        return sphericalFovY;
    }

    public boolean isInjectSphericalMetadata() {
        return injectSphericalMetadata;
    }

    public boolean isDepthMap() {
        return depthMap;
    }

    public boolean isCameraPathExport() {
        return cameraPathExport;
    }

    public AntiAliasing getAntiAliasing() {
        return antiAliasing;
    }

    public String getExportCommand() {
        return exportCommand;
    }

    public String getExportArguments() {
        return exportArguments;
    }

    public boolean isHighPerformance() {
        return highPerformance;
    }

    @Override
    public String toString() {
        return "RenderSettings{" +
                "renderMethod=" + renderMethod +
                ", encodingPreset=" + encodingPreset +
                ", videoWidth=" + videoWidth +
                ", videoHeight=" + videoHeight +
                ", framesPerSecond=" + framesPerSecond +
                ", bitRate=" + bitRate +
                ", outputFile=" + outputFile +
                ", renderNameTags=" + renderNameTags +
                ", includeAlphaChannel=" + includeAlphaChannel +
                ", stabilizeYaw=" + stabilizeYaw +
                ", stabilizePitch=" + stabilizePitch +
                ", stabilizeRoll=" + stabilizeRoll +
                ", chromaKeyingColor=" + chromaKeyingColor +
                ", sphericalFovX=" + sphericalFovX +
                ", sphericalFovY=" + sphericalFovY +
                ", injectSphericalMetadata=" + injectSphericalMetadata +
                ", depthMap=" + depthMap +
                ", cameraPathExport=" + cameraPathExport +
                ", antiAliasing=" + antiAliasing +
                ", exportCommand='" + exportCommand + '\'' +
                ", exportArgumentsPreBgra='" + exportArgumentsPreBgra + '\'' +
                ", exportArguments='" + exportArguments + '\'' +
                ", highPerformance=" + highPerformance +
                '}';
    }
}
