package de.mrjulsen.crn.client.gui.widgets;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;
import net.createmod.catnip.gui.element.ScreenElement;

public class FlatIconButton extends DLButton {    

    public static final int WIDTH = 18;
    public static final int HEIGHT = 18;
    public final Property<ScreenElement> icon = new Property<>(null);
    public final Property<DLSprite> sprite = new Property<>(null);

    public FlatIconButton(int x, int y, ScreenElement icon) {
        super(x, y, WIDTH, HEIGHT);
        this.backgroundTint.set(DLColor.TRANSPARENT);
        this.icon.set(icon);
        this.sprite.set(null);
        this.cursor.set(CursorType.HAND);
        this.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
    }
    
    public FlatIconButton(int x, int y, DLSprite icon) {
        super(x, y, WIDTH, HEIGHT);
        this.backgroundTint.set(DLColor.TRANSPARENT);
        this.icon.set(null);
        this.sprite.set(icon);
        this.cursor.set(CursorType.HAND);
        this.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
    }
    
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.fill(graphics, 0, 0, width(), height(), backgroundTint.get());

        if (!enabled.get()) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.DISABLED);
        } else if (isMouseDown() && (getWindowManager() != null ? getWindowManager().getMouseDownButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT : true)) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.DOWN_SELECTED);
        } else if (isSelected()) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.SELECTED);
        } else {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.NORMAL);
        }
        
        if (icon.get() == null) {
            sprite.get().render(graphics, width() / 2 - sprite.get().getWidth() / 2, height() / 2 - sprite.get().getHeight() / 2);
        } else {
            icon.get().render(graphics.graphics(), 1, 1);
        }
    }
}
