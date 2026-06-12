package github.com.gengyoubo.replayneo.platform.feature.replay.handler;

import github.com.gengyoubo.replayneo.platform.gui.GuiReplayButton;
import github.com.gengyoubo.replayneo.platform.feature.replay.Setting;
import github.com.gengyoubo.replayneo.platform.gui.container.VanillaGuiScreen;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiTooltip;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import github.com.gengyoubo.replayneo.api.callbacks.InitScreenCallback;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.feature.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import github.com.gengyoubo.replayneo.platform.gui.MinecraftGuiRenderer;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;
import static github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay.LOGGER;

public class GuiHandler extends EventRegistrations {
    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private final ReplayModReplay mod;

    public GuiHandler(ReplayModReplay mod) {
        this.mod = mod;
    }

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen, Collection<AbstractButton> buttonList) {
        if (!(guiScreen instanceof PauseScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            final Component BUTTON_OPTIONS = Component.translatable("menu.options");
            final Component BUTTON_EXIT_SERVER = Component.translatable("menu.disconnect");
            final Component BUTTON_ADVANCEMENTS = Component.translatable("gui.advancements");
            final Component BUTTON_STATS = Component.translatable("gui.stats");
            final Component BUTTON_OPEN_TO_LAN = Component.translatable("menu.shareToLan");


            AbstractButton achievements = null, stats = null;
            for(AbstractButton b : new ArrayList<>(buttonList)) {
                boolean remove = false;
                Component id = b.getMessage();
                if (id.equals(BUTTON_EXIT_SERVER)) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    remove = true;
                    addButton(guiScreen, new InjectedButton(
                            guiScreen,
                            BUTTON_EXIT_REPLAY,
                            b.getX(),
                            b.getY(),
                            b.getWidth(),
                            b.getHeight(),
                            "replaymod.gui.exit",
                            null,
                            this::onButton
                    ));
                } else if (id.equals(BUTTON_ADVANCEMENTS)) {
                    // Remove "Advancements", "Stats" and "Open to LAN" buttons
                    remove = true;
                    achievements = b;
                } else if (id.equals(BUTTON_STATS)) {
                    remove = true;
                    stats = b;
                } else if (id.equals(BUTTON_OPEN_TO_LAN)) {
                    remove = true;
                }
                if (remove) {
                    // Moving the button far off-screen is easier to do cross-version than actually removing it
                    b.setX(-1000);
                    b.setY(-1000);
                }
            }
            if (achievements != null && stats != null) {
                moveAllButtonsInRect(buttonList,
                        achievements.getX(), stats.getX() + stats.getWidth(),
                        achievements.getY(), Integer.MAX_VALUE
                );
            }
            // In 1.13+ Forge, the Options button shares one row with the Open to LAN button
        }
    }

    /**
     * Moves all buttons that in any way intersect a rectangle by a given amount on the y axis.
     *
     * @param buttons List of buttons
     * @param xStart  Left x limit of the rectangle
     * @param xEnd    Right x limit of the rectangle
     * @param yStart  Top y limit of the rectangle
     * @param yEnd    Bottom y limit of the rectangle
     */
    private void moveAllButtonsInRect(
            Collection<AbstractButton> buttons,
            int xStart,
            int xEnd,
            int yStart,
            int yEnd
    ) {
        buttons.stream()
                .filter(button -> button.getX() <= xEnd && button.getX() + button.getWidth() >= xStart)
                .filter(button -> button.getY() <= yEnd && button.getY() + button.getHeight() >= yStart)
                // FIXME remap bug: needs the {} to recognize the setter (it also doesn't understand +=)
                .forEach(button -> button.setY(button.getY() + -24));
    }

    { on(InitScreenCallback.EVENT, (screen, buttons) -> ensureReplayStopped(screen)); }
    private void ensureReplayStopped(Screen guiScreen) {
        if (!(guiScreen instanceof TitleScreen || guiScreen instanceof JoinMultiplayerScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Something went terribly wrong and we ended up in the main menu with the replay still active.
            // To prevent players from joining live servers and using the CameraEntity, try to stop the replay now.
            try {
                mod.getReplayHandler().endReplay();
            } catch (IOException e) {
                LOGGER.error("Trying to stop broken replay: ", e);
            } finally {
                if (mod.getReplayHandler() != null) {
                    mod.forcefullyStopReplay();
                }
            }
        }
    }

    { on(InitScreenCallback.EVENT, this::injectIntoMainMenu); }
    private void injectIntoMainMenu(Screen guiScreen, Collection<AbstractButton> buttonList) {
        if (!(guiScreen instanceof TitleScreen)) {
            return;
        }

        if (mod.getCore().getSettingsRegistry().get(Setting.LEGACY_MAIN_MENU_BUTTON)) {
            legacyInjectIntoMainMenu(guiScreen, buttonList);
        } else {
            properInjectIntoMainMenu(guiScreen);
        }
    }

    private void properInjectIntoMainMenu(Screen screen) {
        List<AbstractButton> buttonList = getButtons(screen);
        MainMenuButtonPosition buttonPosition = MainMenuButtonPosition.valueOf(mod.getCore().getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON));

        // Workaround for FancyMenu v2 initializing the screen twice, likely fixed in v3
        if (isFancyMenu2Installed()) {
            for (AbstractButton button : buttonList) {
                if (button instanceof InjectedButton) {
                    return;
                }
            }
        }

        Point pos;
        if (buttonPosition == MainMenuButtonPosition.BIG) {
            int x = screen.width / 2 - 100;
            // We want to position our button below the realms button
            Optional<AbstractButton> targetButton = findButton(buttonList, "menu.online", 14).or(() -> findButton(buttonList, "menu.multiplayer", 2));

            int y = targetButton
                    // if we found some button, put our button at its position (we'll move it out of the way shortly)
                    .map(AbstractWidget::getY)
                    // and if we can't even find that one, then just guess
                    .orElse(screen.height / 4 + 10 + 4 * 24);

            // Move all buttons above or at our one upwards
            moveAllButtonsInRect(buttonList,
                    x, x + 200,
                    Integer.MIN_VALUE, y
            );

            pos = new Point(x, y);
        } else {
            pos = determineButtonPos(buttonPosition, screen, buttonList);
        }

        AbstractButton replayViewerButton;
        if (buttonPosition == MainMenuButtonPosition.BIG) {
            replayViewerButton = new InjectedButton(
                    screen, BUTTON_REPLAY_VIEWER,
                    pos.getX(), pos.getY(),
                    200, 20,
                    "replaymod.gui.replayviewer",
                    null,
                    this::onButton
            );
        } else {
            replayViewerButton = new InjectedButton(
                    screen, BUTTON_REPLAY_VIEWER,
                    pos.getX(), pos.getY(),
                    20, 20,
                    "",
                    "replaymod.gui.replayviewer",
                    this::onButton
            ) {
                @Override
                public void renderWidget(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
                    super.renderWidget(context, mouseX, mouseY, delta);

                    MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(context);
                    renderer.bindTexture(GuiReplayButton.ICON);
                    renderer.drawTexturedRect(
                            this.getX() + 3, this.getY() + 3,
                            0, 0,
                            this.width - 6, this.height - 6,
                            1, 1,
                            1, 1
                    );
                }
            };
        }

        addButton(screen, replayViewerButton);
    }

    private boolean isFancyMenu2Installed() {
        return ModList.get().getModContainerById("fancymenu")
                .map(mod -> mod.getModInfo().getVersion().toString().startsWith("2."))
                .orElse(false);
    }

    private static List<AbstractButton> getButtons(Screen screen) {
        List<AbstractButton> buttons = new ArrayList<>();
        for (var child : screen.children()) {
            if (child instanceof AbstractButton button) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    private void legacyInjectIntoMainMenu(Screen guiScreen, Collection<AbstractButton> buttonList) {
        boolean isCustomMainMenuMod = guiScreen.getClass().getName().endsWith("custommainmenu.gui.GuiFakeMain");

        MainMenuButtonPosition buttonPosition = MainMenuButtonPosition.valueOf(mod.getCore().getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON));
        if (buttonPosition != MainMenuButtonPosition.BIG && !isCustomMainMenuMod) {
            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(guiScreen);

            GuiReplayButton replayButton = new GuiReplayButton();
            replayButton
                    .onClick(() -> new GuiReplayViewer(mod).display())
                    .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.replayviewer"));

            vanillaGui.setLayout(new CustomLayout<github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen>(vanillaGui.getLayout()) {
                private Point pos;

                @Override
                protected void layout(github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen container, int width, int height) {
                    if (pos == null) {
                        // Delaying computation so we can take into account buttons
                        // added after our callback.
                        pos = determineButtonPos(buttonPosition, guiScreen, buttonList);
                    }
                    size(replayButton, 20, 20);
                    pos(replayButton, pos.getX(), pos.getY());
                }
            }).addElements(null, replayButton);
            return;
        }

        int x = guiScreen.width / 2 - 100;
        // We want to position our button below the realms button
        int y = findButton(buttonList, "menu.online", 14).or(() -> findButton(buttonList, "menu.multiplayer", 2))
                // if we found some button, put our button at its position (we'll move it out of the way shortly)
                .map(AbstractWidget::getY)
                // and if we can't even find that one, then just guess
                .orElse(guiScreen.height / 4 + 10 + 4 * 24);

        // Move all buttons above or at our one upwards
        moveAllButtonsInRect(buttonList,
                x, x + 200,
                Integer.MIN_VALUE, y
        );

        // Add our button
        InjectedButton button = new InjectedButton(
                guiScreen,
                BUTTON_REPLAY_VIEWER,
                x,
                y,
                200,
                20,
                "replaymod.gui.replayviewer",
                null,
                this::onButton
        );
        addButton(guiScreen, button);
    }

    private Point determineButtonPos(MainMenuButtonPosition buttonPosition, Screen guiScreen, Collection<AbstractButton> buttonList) {
        Point topRight = new Point(guiScreen.width - 20 - 5, 5);

        if (buttonPosition == MainMenuButtonPosition.TOP_LEFT) {
            return new Point(5, 5);
        } else if (buttonPosition == MainMenuButtonPosition.TOP_RIGHT) {
            return topRight;
        } else if (buttonPosition == MainMenuButtonPosition.DEFAULT) {
            return Stream.of(
                    findButton(buttonList, "menu.singleplayer", 1),
                    findButton(buttonList, "menu.multiplayer", 2),
                    findButton(buttonList, "menu.online", 14),
                    findButton(buttonList, "modmenu.title", 6)
            )
                    // skip buttons which do not exist
                    .flatMap(Optional::stream)
                    // skip buttons which already have something next to them
                    .filter(it -> buttonList.stream().noneMatch(button ->
                            button.getX() <= it.getX() + it.getWidth() + 4 + 20
                                    && button.getY() <= it.getY() + it.getHeight()
                                    && button.getX() + button.getWidth() >= it.getX() + it.getWidth() + 4
                                    && button.getY() + button.getHeight() >= it.getY()
                    ))
                    // then take the bottom-most and if there's two, the right-most
                    .max(Comparator.<AbstractButton>comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX))
                    // and place ourselves next to it
                    .map(it -> new Point(it.getX() + it.getWidth() + 4, it.getY()))
                    // if all fails, just go with TOP_RIGHT
                    .orElse(topRight);
        } else {
            return Optional.of(buttonList).flatMap(buttons -> switch (buttonPosition) {
                case LEFT_OF_SINGLEPLAYER, RIGHT_OF_SINGLEPLAYER -> findButton(buttons, "menu.singleplayer", 1);
                case LEFT_OF_MULTIPLAYER, RIGHT_OF_MULTIPLAYER -> findButton(buttons, "menu.multiplayer", 2);
                case LEFT_OF_REALMS, RIGHT_OF_REALMS -> findButton(buttons, "menu.online", 14);
                case LEFT_OF_MODS, RIGHT_OF_MODS -> findButton(buttons, "modmenu.title", 6);
                default -> throw new RuntimeException();
            }).map(button -> switch (buttonPosition) {
                case LEFT_OF_SINGLEPLAYER, LEFT_OF_MULTIPLAYER, LEFT_OF_REALMS, LEFT_OF_MODS ->
                        new Point(button.getX() - 4 - 20, button.getY());
                case RIGHT_OF_MODS, RIGHT_OF_SINGLEPLAYER, RIGHT_OF_MULTIPLAYER, RIGHT_OF_REALMS ->
                        new Point(button.getX() + button.getWidth() + 4, button.getY());
                default -> throw new RuntimeException();
            }).orElse(topRight);
        }
    }

    private int determineButtonIndex(Collection<AbstractButton> buttons, AbstractButton button) {
        AbstractButton best = null;
        int bestIndex = -1;

        int index = 0;
        for (AbstractButton other : buttons) {
            if (other.getY() > button.getY() || other.getY() == button.getY() && other.getX() > button.getX()) {
                index++;
                continue;
            }

            if (best == null || other.getY() > best.getY() || other.getY() == best.getY() && other.getX() > best.getX()) {
                best = other;
                bestIndex = index + 1;
            }

            index++;
        }
        return bestIndex;
    }

    private void onButton(InjectedButton button) {
        Screen guiScreen = button.guiScreen;
        if(!button.active) return;

        if (guiScreen instanceof TitleScreen) {
            if (button.id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
            }
        }

        if (guiScreen instanceof PauseScreen && mod.getReplayHandler() != null) {
            if (button.id == BUTTON_EXIT_REPLAY) {
                button.active = false;
                try {
                    mod.getReplayHandler().endReplay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class InjectedButton extends
            Button
    {
        public final Screen guiScreen;
        public final int id;

        public InjectedButton(Screen guiScreen, int buttonId, int x, int y, int width, int height, String buttonText,
                              String tooltip,
                              Consumer<InjectedButton> onClick
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.translatable(buttonText)
                    , self -> onClick.accept((InjectedButton) self)
                    , Button.DEFAULT_NARRATION
            );
            this.guiScreen = guiScreen;
            this.id = buttonId;

            if (tooltip != null) {
                setTooltip(Tooltip.create(net.minecraft.network.chat.Component.translatable(tooltip)));
            }
        }

    }

    public enum MainMenuButtonPosition {
        // The old big button below Realms/Mods which pushes other buttons around.
        BIG,
        // Right of the bottom-most button in the main block of buttons (so not the quit button).
        // That will generally be either RIGHT_OF_REALMS or RIGHT_OF_MODS depending on version and installed mods.
        DEFAULT,
        TOP_LEFT,
        TOP_RIGHT,
        LEFT_OF_SINGLEPLAYER,
        RIGHT_OF_SINGLEPLAYER,
        LEFT_OF_MULTIPLAYER,
        RIGHT_OF_MULTIPLAYER,
        LEFT_OF_REALMS,
        RIGHT_OF_REALMS,
        LEFT_OF_MODS,
        RIGHT_OF_MODS,
    }
}
