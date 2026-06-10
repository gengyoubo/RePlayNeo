package github.com.gengyoubo.replayneo.platform.addon;
import github.com.gengyoubo.replayneo.api.Extra;
import github.com.gengyoubo.replayneo.platform.gui.ReplayTextures;

import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.platform.feature.render.events.ReplayOpenedCallback;
import github.com.gengyoubo.replayneo.platform.feature.replay.gui.overlay.GuiReplayOverlay;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
import github.com.gengyoubo.replayneo.api.gui.element.GuiElement;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiLabel;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiTooltip;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.GridLayout;
import github.com.gengyoubo.replayneo.api.layout.LayoutData;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.client.resources.language.I18n;

public class HotkeyButtons extends EventRegistrations implements Extra {
    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) {
        this.mod = mod;

        register();
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> new Gui(mod, replayHandler.getOverlay())); }
    public static final class Gui {
        private final GuiButton toggleButton;
        private final GridLayout panelLayout;
        private final GuiPanel panel;

        private boolean open;

        public Gui(ReplayMod mod, GuiReplayOverlay overlay) {
            toggleButton = new GuiButton(overlay).setSize(20, 20)
                    .setTexture(ReplayTextures.TEXTURE, ReplayTextures.TEXTURE_SIZE).setSpriteUV(0, 120)
                    .onClick(() -> open = !open);

            panel = new GuiPanel(overlay) {
                @Override
                public Collection<GuiElement> getChildren() {
                    return open ? super.getChildren() : Collections.emptyList();
                }

                @Override
                public Map<GuiElement, LayoutData> getElements() {
                    return open ? super.getElements() : Collections.emptyMap();
                }
            }.setLayout(panelLayout = new GridLayout().setSpacingX(5).setSpacingY(5).setColumns(1));

            final ReplayKeyBindingRegistry keyBindingRegistry = mod.getKeyBindingRegistry();
            keyBindingRegistry.getBindings().values().stream()
                    .sorted(Comparator.comparing(it -> I18n.get(it.name())))
                    .forEachOrdered(KeyMapping -> {
                GuiButton button = new GuiButton(){
                    @Override
                    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                        // There doesn't seem to be an KeyBindingUpdate event, so we'll just update it every time
                        setLabel(KeyMapping.isBound() ? KeyMapping.getBoundKey() : "");

                        if (KeyMapping.supportsAutoActivation()) {
                            setTooltip(new GuiTooltip().setText(new String[]{
                                    I18n.get("replaymod.gui.ingame.autoactivating"),
                                    I18n.get("replaymod.gui.ingame.autoactivating."
                                            + (KeyMapping.isAutoActivating() ? "disable" : "enable")),
                            }));
                            setLabelColor(KeyMapping.isAutoActivating() ? 0x00ff00 : 0xe0e0e0);
                        }

                        super.draw(renderer, size, renderInfo);
                    }
                }.onClick(click -> {
                    if (KeyMapping.supportsAutoActivation() && click.hasCtrl()) {
                        KeyMapping.setAutoActivating(!KeyMapping.isAutoActivating());
                    } else {
                        KeyMapping.trigger();
                    }
                });
                GuiLabel label = new GuiLabel().setI18nText(KeyMapping.name());
                panel.addElements(null, new GuiPanel().setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        width(button, Math.max(10 /* consistent min width */, width(button)) + 10 /* padding */);
                        height(button, 20);

                        int textWidth = width(label);

                        x(label, width(button) + 4);
                        width(label, width - x(label));

                        if (textWidth > width - x(label)) {
                            height(label, height(label) * 2); // split over two lines
                        }
                        y(label, (height - height(label)) / 2);
                    }
                }).addElements(null, button, label).setSize(150, 20));
            });

            overlay.setLayout(new CustomLayout<GuiReplayOverlay>(overlay.getLayout()) {
                @Override
                protected void layout(GuiReplayOverlay container, int width, int height) {
                    panelLayout.setColumns(Math.max(1, (width - 10) / 155));
                    size(panel, panel.getMinSize());

                    pos(toggleButton, 5, height - 25);
                    pos(panel, 5, y(toggleButton) - 5 - height(panel));
                }
            });
        }
    }
}
