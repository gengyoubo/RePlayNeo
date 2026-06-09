package github.com.gengyoubo.replayneo.core.gui;

import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScreen;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiButton;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiElement;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiLabel;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiToggleButton;
import github.com.gengyoubo.replayneo.feature.pathing.element.advanced.GuiDropdownMenu;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.VerticalLayout;
import github.com.gengyoubo.replayneo.core.utils.Consumer;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class GuiReplaySettings extends AbstractGuiScreen<GuiReplaySettings> {

    public GuiReplaySettings(final net.minecraft.client.gui.screens.Screen parent, final SettingsRegistry settingsRegistry) {
        final GuiButton doneButton = new GuiButton(this).setI18nLabel("gui.done").setSize(200, 20).onClick(() -> getMinecraft().setScreen(parent));

        final GuiPanel allElements = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(10));
        GuiPanel leftColumn = new GuiPanel().setLayout(new VerticalLayout().setSpacing(4));
        GuiPanel rightColumn = new GuiPanel().setLayout(new VerticalLayout().setSpacing(4));
        allElements.addElements(new VerticalLayout.Data(0), leftColumn, rightColumn);
        HorizontalLayout.Data leftHorizontalData = new HorizontalLayout.Data(1);
        HorizontalLayout.Data rightHorizontalData = new HorizontalLayout.Data(0);
        int i = 0;
        for (final SettingsRegistry.SettingKey<?> key : settingsRegistry.getSettings()) {
            if (key.getDisplayString() != null) {
                GuiElement<?> element;
                if (key.getDefault() instanceof Boolean) {
                    @SuppressWarnings("unchecked")
                    final SettingsRegistry.SettingKey<Boolean> booleanKey = (SettingsRegistry.SettingKey<Boolean>) key;
                    final GuiToggleButton<Object> button = new GuiToggleButton<>().setSize(150, 20)
                            .setI18nLabel(key.getDisplayString()).setSelected(settingsRegistry.get(booleanKey) ? 0 : 1)
                            .setValues(translate("options.on"), Arrays.toString(new String[]{translate("options.off")}));
                    element = button.onClick(() -> {
                        settingsRegistry.set(booleanKey, button.getSelected() == 0);
                        settingsRegistry.save();
                    });
                } else if (key instanceof SettingsRegistry.MultipleChoiceSettingKey<?> multipleChoiceKey) {
                    List<?> values = multipleChoiceKey.getChoices();
                    MultipleChoiceDropdownEntry[] entries = new MultipleChoiceDropdownEntry[values.size()];
                    int selected = 0;
                    Object currentValue = settingsRegistry.get(multipleChoiceKey);
                    for (int j = 0; j < entries.length; j++) {
                        Object value = values.get(j);
                        entries[j] = new MultipleChoiceDropdownEntry(value,
                                translate(multipleChoiceKey.getDisplayString()) + ": " + translate(value.toString()));
                        if (currentValue.equals(value)) {
                            selected = j;
                        }
                    }
                    final GuiDropdownMenu<MultipleChoiceDropdownEntry> menu = new GuiDropdownMenu<MultipleChoiceDropdownEntry>() {
                        @Override
                        protected ReadableDimension calcMinSize() {
                            ReadableDimension size = super.calcMinSize();
                            if (size.getWidth() > 150) {
                                return new Dimension(150, size.getHeight());
                            } else {
                                return size;
                            }
                        }
                    }.setSize(150, 20).setValues(entries);
                    menu.setSelected(selected).onSelection(obj -> {
                        settingsRegistry.set((SettingsRegistry.SettingKey) multipleChoiceKey,
                                menu.getSelectedValue().value);
                        settingsRegistry.save();
                    });
                    element = menu;
                } else {
                    throw new IllegalArgumentException("Type " + key.getDefault().getClass() + " not supported.");
                }

                if (i++ % 2 == 0) {
                    leftColumn.addElements(leftHorizontalData, element);
                } else {
                    rightColumn.addElements(rightHorizontalData, element);
                }
            }
        }

        setLayout(new CustomLayout<GuiReplaySettings>() {
            @Override
            protected void layout(GuiReplaySettings container, int width, int height) {
                pos(allElements, width / 2 - 155, height / 6);
                pos(doneButton, width / 2 - 100, height - 27);
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.settings.title"));
    }

    @Override
    protected GuiReplaySettings getThis() {
        return this;
    }

    private record MultipleChoiceDropdownEntry(Object value, String text) {

        @Override
        public @NotNull String toString() {
            return text;
        }
    }

    private static String translate(String key, Object... args) {
        return ReplayPlatforms.get().client().translate(key, args);
    }
}
