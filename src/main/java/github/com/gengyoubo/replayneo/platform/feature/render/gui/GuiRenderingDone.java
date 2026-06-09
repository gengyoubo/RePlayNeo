package github.com.gengyoubo.replayneo.platform.feature.render.gui;

import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.render.RenderSettings;
import github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender;
import github.com.gengyoubo.replayneo.platform.feature.render.Setting;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.core.gui.container.GuiScreen;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiButton;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiCheckbox;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiLabel;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.VerticalLayout;

import java.io.File;

public class GuiRenderingDone extends GuiScreen {
    public final ReplayModRender mod;
    public final File videoFile;
    public final int videoFrames;
    public final RenderSettings settings;

    public final GuiLabel infoLine1 = new GuiLabel().setI18nText("replaymod.gui.renderdone1");
    public final GuiLabel infoLine2 = new GuiLabel().setI18nText("replaymod.gui.renderdone2");

    public final GuiButton openFolder = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            MCVer.openFile(videoFile.getParentFile());
        }
    }).setSize(200, 20).setI18nLabel("replaymod.gui.openfolder");

    public final GuiPanel actionsPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10))
            .addElements(null, openFolder);

    public final GuiPanel mainPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(10))
            .addElements(new VerticalLayout.Data(0.5),
                    new GuiPanel().setLayout(new VerticalLayout().setSpacing(4)).addElements(null, infoLine1, infoLine2),
                    actionsPanel);

    public final GuiButton closeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            if (neverOpenCheckbox.isChecked()) {
                SettingsRegistry settingsRegistry = mod.getCore().getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_RENDER_GUI, true);
                settingsRegistry.save();
            }
            getMinecraft().setScreen(null);
        }
    }).setSize(100, 20).setI18nLabel("replaymod.gui.close");

    public final GuiCheckbox neverOpenCheckbox = new GuiCheckbox().setI18nLabel("replaymod.gui.notagain");

    public final GuiPanel closePanel = new GuiPanel(this)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5))
            .addElements(new HorizontalLayout.Data(0.5), neverOpenCheckbox, closeButton);

    {
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(mainPanel, width / 2 - width(mainPanel) / 2, height / 3 - height(mainPanel) / 2);
                pos(closePanel, width - 10 - width(closePanel), height - 10 - height(closePanel));
            }
        });
        setTitle(new GuiLabel().setI18nText("replaymod.gui.renderdonetitle"));
        setBackground(Background.DIRT);
    }

    public GuiRenderingDone(ReplayModRender mod, File videoFile, int videoFrames, RenderSettings settings) {
        this.mod = mod;
        this.videoFile = videoFile;
        this.videoFrames = videoFrames;
        this.settings = settings;
    }

    @Override
    public void display() {
        if (mod.getCore().getSettingsRegistry().get(Setting.SKIP_POST_RENDER_GUI)) {
            return;
        }
        super.display();
    }
}
