package com.replaymod.replay.handler;

import com.replaymod.core.gui.GuiReplayButton;
import com.replaymod.replay.Setting;
import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import de.johni0702.minecraft.gui.MinecraftGuiRenderer;
import net.minecraftforge.fml.ModList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

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
                if (id == null) {
                    // likely a button of some third-part mod
                    // e.g. https://github.com/Pokechu22/WorldDownloader/blob/b1b279f948beec2d7dac7524eea8f584a866d8eb/share_14/src/main/java/wdl/WDLHooks.java#L491
                    continue;
                }
                if (id.equals(BUTTON_EXIT_SERVER)) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    remove = true;
                    addButton(guiScreen, new InjectedButton(
                            guiScreen,
                            BUTTON_EXIT_REPLAY,
                            b.x,
                            b.y,
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
                    b.x = -1000;
                    b.y = -1000;
                }
            }
            if (achievements != null && stats != null) {
                moveAllButtonsInRect(buttonList,
                        achievements.x, stats.x + stats.getWidth(),
                        achievements.y, Integer.MAX_VALUE,
                        -24);
            }
            // In 1.13+ Forge, the Options button shares one row with the Open to LAN button
        }
    }

    /**
     * Moves all buttons that in any way intersect a rectangle by a given amount on the y axis.
     * @param buttons List of buttons
     * @param yStart Top y limit of the rectangle
     * @param yEnd Bottom y limit of the rectangle
     * @param xStart Left x limit of the rectangle
     * @param xEnd Right x limit of the rectangle
     * @param moveBy Signed distance to move the buttons
     */
    private void moveAllButtonsInRect(
            Collection<AbstractButton> buttons,
            int xStart,
            int xEnd,
            int yStart,
            int yEnd,
            int moveBy
    ) {
        buttons.stream()
                .filter(button -> button.x <= xEnd && button.x + button.getWidth() >= xStart)
                .filter(button -> button.y <= yEnd && button.y + button.getHeight() >= yStart)
                // FIXME remap bug: needs the {} to recognize the setter (it also doesn't understand +=)
                .forEach(button -> { button.y = button.y + moveBy; });
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
            Optional<AbstractButton> targetButton = findButton(buttonList, "menu.online", 14)
                    .map(Optional::of)
                    // or, if someone removed the realms button, we'll alternatively take the multiplayer one
                    .orElseGet(() -> findButton(buttonList, "menu.multiplayer", 2));

            int y = targetButton
                    // if we found some button, put our button at its position (we'll move it out of the way shortly)
                    .map(it -> it.y)
                    // and if we can't even find that one, then just guess
                    .orElse(screen.height / 4 + 10 + 4 * 24);

            // Move all buttons above or at our one upwards
            moveAllButtonsInRect(buttonList,
                    x, x + 200,
                    Integer.MIN_VALUE, y,
                    -24);

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
                public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
                    super.renderWidget(context, mouseX, mouseY, delta);

                    MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(context);
                    renderer.bindTexture(GuiReplayButton.ICON);
                    renderer.drawTexturedRect(
                            this.x + 3, this.y + 3,
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

            vanillaGui.setLayout(new CustomLayout<de.johni0702.minecraft.gui.container.GuiScreen>(vanillaGui.getLayout()) {
                private Point pos;

                @Override
                protected void layout(de.johni0702.minecraft.gui.container.GuiScreen container, int width, int height) {
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
        int y = findButton(buttonList, "menu.online", 14)
                .map(Optional::of)
                // or, if someone removed the realms button, we'll alternatively take the multiplayer one
                .orElse(findButton(buttonList, "menu.multiplayer", 2))
                // if we found some button, put our button at its position (we'll move it out of the way shortly)
                .map(it -> it.y)
                // and if we can't even find that one, then just guess
                .orElse(guiScreen.height / 4 + 10 + 4 * 24);

        // Move all buttons above or at our one upwards
        moveAllButtonsInRect(buttonList,
                x, x + 200,
                Integer.MIN_VALUE, y,
                -24);

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
                    .flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty))
                    // skip buttons which already have something next to them
                    .filter(it -> buttonList.stream().noneMatch(button ->
                            button.x <= it.x + it.getWidth() + 4 + 20
                                    && button.y <= it.y + it.getHeight()
                                    && button.x + button.getWidth() >= it.x + it.getWidth() + 4
                                    && button.y + button.getHeight() >= it.y
                    ))
                    // then take the bottom-most and if there's two, the right-most
                    .max(Comparator.<AbstractButton>comparingInt(it -> it.y).thenComparingInt(it -> it.x))
                    // and place ourselves next to it
                    .map(it -> new Point(it.x + it.getWidth() + 4, it.y))
                    // if all fails, just go with TOP_RIGHT
                    .orElse(topRight);
        } else {
            return Optional.of(buttonList).flatMap(buttons -> {
                switch (buttonPosition) {
                    case LEFT_OF_SINGLEPLAYER:
                    case RIGHT_OF_SINGLEPLAYER:
                        return findButton(buttons, "menu.singleplayer", 1);
                    case LEFT_OF_MULTIPLAYER:
                    case RIGHT_OF_MULTIPLAYER:
                        return findButton(buttons, "menu.multiplayer", 2);
                    case LEFT_OF_REALMS:
                    case RIGHT_OF_REALMS:
                        return findButton(buttons, "menu.online", 14);
                    case LEFT_OF_MODS:
                    case RIGHT_OF_MODS:
                        return findButton(buttons, "modmenu.title", 6);
                }
                throw new RuntimeException();
            }).map(button -> {
                switch (buttonPosition) {
                    case LEFT_OF_SINGLEPLAYER:
                    case LEFT_OF_MULTIPLAYER:
                    case LEFT_OF_REALMS:
                    case LEFT_OF_MODS:
                        return new Point(button.x - 4 - 20, button.y);
                    case RIGHT_OF_MODS:
                    case RIGHT_OF_SINGLEPLAYER:
                    case RIGHT_OF_MULTIPLAYER:
                    case RIGHT_OF_REALMS:
                        return new Point(button.x + button.getWidth() + 4, button.y);
                }
                throw new RuntimeException();
            }).orElse(topRight);
        }
    }

    private int determineButtonIndex(Collection<AbstractButton> buttons, AbstractButton button) {
        AbstractButton best = null;
        int bestIndex = -1;

        int index = 0;
        for (AbstractButton other : buttons) {
            if (other.y > button.y || other.y == button.y && other.x > button.x) {
                index++;
                continue;
            }

            if (best == null || other.y > best.y || other.y == best.y && other.x > best.x) {
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
        private Consumer<InjectedButton> onClick;
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
            this.onClick = onClick;

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
