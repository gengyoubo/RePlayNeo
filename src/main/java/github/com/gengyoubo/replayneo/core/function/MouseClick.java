package github.com.gengyoubo.replayneo.core.function;

import de.johni0702.minecraft.gui.utils.lwjgl.WritablePoint;
import github.com.gengyoubo.replayneo.api.function.Click;
import github.com.gengyoubo.replayneo.api.function.InputWithModifiers;

public record MouseClick(int x, int y, int button, int modifiers) implements Click {
    public MouseClick(double x, double y, int button) {
        this((int) Math.round(x), (int) Math.round(y), button, InputWithModifiers.currentModifiers());
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void getLocation(WritablePoint writablePoint) {
        writablePoint.setLocation(x, y);
    }
}
