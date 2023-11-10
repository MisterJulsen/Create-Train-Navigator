package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.ITickableWidget;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class NewTextEntryWidget extends Button implements ITickableWidget, IForegroundRendering {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png"); 
    public static final int WIDTH = 200;
    public static final int HEIGHT = 28;   

    private final Runnable onUpdate;
    private final Supplier<Integer> getScrollOffset;
    private final Screen parent;

    // Controls
    private final ModEditBox newEntryBox;
    private GuiAreaDefinition addButton;

    // Tooltips
    private final TranslatableComponent tooltipAdd = new TranslatableComponent("gui." + ModMain.MOD_ID + ".new_text_entry.add.tooltip");
    

    public NewTextEntryWidget(Screen parent, int pX, int pY, Runnable onFocusLost, Runnable onUpdate, Supplier<Integer> scrollOffset) {
        super(pX, pY, WIDTH, HEIGHT, new TextComponent(""), (btn) -> {});
        
        Minecraft minecraft = Minecraft.getInstance();
        this.onUpdate = onUpdate;
        this.parent = parent;
        this.getScrollOffset = scrollOffset;
        
        newEntryBox = new ModEditBox(minecraft.font, pX + 30, pY + 30, 129, 12, new TextComponent(""));
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(25);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.setFocus(true);
        newEntryBox.setOnFocusChanged((box, focus) -> {
            if (!focus) {
                onFocusLost.run();
            }
        });

        setY(pY);
    }    

    public void setY(int y) {
        this.y = y;
        newEntryBox.y = y + 10;
        addButton = new GuiAreaDefinition(x + 165, y + 6, 16, 16);
    }

    @Override
    public void tick() {
        newEntryBox.tick();
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {     
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 110, WIDTH, HEIGHT);
        blit(pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 
        blit(pPoseStack, x + 25, y + 5, 0, 92, 139, 18); // textbox

        if (addButton.isInBounds(pMouseX, pMouseY)) { 
            fill(pPoseStack, addButton.getX(), addButton.getY(), addButton.getRight(), addButton.getBottom(), 0x1AFFFFFF);
        }

        newEntryBox.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
    
    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {		
        GuiUtils.renderTooltip(parent, addButton, List.of(tooltipAdd.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, this.getScrollOffset.get()); 
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (addButton.isInBounds(pMouseX, pMouseY) && !newEntryBox.getValue().isBlank()) {
            GlobalSettingsManager.getInstance().getSettingsData().registerAlias(new TrainStationAlias(AliasName.of(newEntryBox.getValue())), onUpdate);
            newEntryBox.setFocus(false);
            onUpdate.run();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
        
        newEntryBox.mouseClicked(pMouseX, pMouseY, pButton);
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        newEntryBox.keyPressed(pKeyCode, pScanCode, pModifiers);
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        newEntryBox.charTyped(pCodePoint, pModifiers);
        return super.charTyped(pCodePoint, pModifiers);
    }
}
