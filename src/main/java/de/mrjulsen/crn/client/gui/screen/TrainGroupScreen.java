package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.ITickableWidget;
import de.mrjulsen.crn.client.gui.widgets.NewTextEntryWidget;
import de.mrjulsen.crn.client.gui.widgets.TrainGroupEntryWidget;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class TrainGroupScreen extends Screen implements IForegroundRendering {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 10;
    private final int ENTRY_SPACING = 4;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 16;        
    private final int AREA_W = 220;
    private final int AREA_H = 194;

    private int guiLeft, guiTop;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private final TrainGroupScreen instance;
    private boolean initEntries = false;
    private boolean renderingEntries = false;

    // Controls
    private final ControlCollection entriesCollection = new ControlCollection();
    private NewTextEntryWidget newTrainGroupWidget;
    private IconButton backButton;
    private IconButton addButton;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Tooltips
    private final TranslatableComponent tooltipAdd = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.add.tooltip");

    @SuppressWarnings("resource")
    public TrainGroupScreen(Level level, Screen lastScreen) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.title"));
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
        this.instance = this;
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;      

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });

        addButton = this.addRenderableWidget(new IconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_ADD) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                if (newTrainGroupWidget != null) {                       
                    removeWidget(newTrainGroupWidget);
                }
                
                newTrainGroupWidget = new NewTextEntryWidget(instance, guiLeft + AREA_X + 10, guiTop, () -> {    
                    removeWidget(newTrainGroupWidget);        
                    newTrainGroupWidget = null;
                }, () -> refreshEntries(), () -> (int)getScrollOffset(1), value -> {
                    GlobalSettingsManager.getInstance().getSettingsData().registerTrainGroup(new TrainGroup(value), () -> refreshEntries());
                });
                scroll.chase(getScrollMax(), 0.7f, Chaser.EXP);
            }
        });

        refreshEntries();
    }

    private void refreshEntries() {
        initEntries = true;
        
        while (renderingEntries) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        

        int startY = guiTop + AREA_X + ENTRIES_START_Y_OFFSET; 

        Collection<String> expandedGroupNames = entriesCollection.components.stream().filter(x -> x instanceof TrainGroupEntryWidget && ((TrainGroupEntryWidget)x).isExpanded()).map(x -> ((TrainGroupEntryWidget)x).getTrainGroup().getGroupName()).toList();
        entriesCollection.components.forEach(x -> removeWidget(x));
        entriesCollection.components.clear();

        // Entries
        TrainGroup[] groups = GlobalSettingsManager.getInstance().getSettingsData().getTrainGroupsList().toArray(TrainGroup[]::new);
        for (int i = 0; i < groups.length; i++) {
            TrainGroup group = groups[i];
            
            TrainGroupEntryWidget w = new TrainGroupEntryWidget(this, guiLeft + AREA_X + 10, startY, group, () -> refreshEntries(), expandedGroupNames.contains(group.getGroupName()));
            entriesCollection.components.add(w);
        }
        initEntries = false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
		scroll.tickChaser();
        entriesCollection.performForEachOfType(ITickableWidget.class, x -> x.tick());
    }

    public int getScrollOffset(float pPartialTicks) {
        return (int)scroll.getValue(pPartialTicks);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);
        
        
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        renderingEntries = true;
         while (initEntries) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // render entries
        int currentY = guiTop + AREA_X + ENTRIES_START_Y_OFFSET;
        for (int i = 0; i < entriesCollection.components.size(); i++) {
            if (entriesCollection.components.get(i) instanceof TrainGroupEntryWidget widget) {
                widget.setY(currentY);
                widget.calcHeight();
                if (currentY < guiTop + AREA_Y + AREA_H - scrollOffset && currentY + widget.getHeight() > guiTop + AREA_Y - scrollOffset) {
                    widget.render(pPoseStack, pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
                }
                currentY += widget.getHeight() + ENTRY_SPACING;
            }
        }

        if (newTrainGroupWidget != null) {
            newTrainGroupWidget.setY(currentY);
            newTrainGroupWidget.render(pPoseStack, pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
        }
        renderingEntries = false;

        pPoseStack.popPose();
        ModGuiUtils.endStencil();        
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10, 0x77000000, 0x00000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H, 0x00000000, 0x77000000);
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        // Scrollbar
        int componentHeight = entriesCollection.components.stream().mapToInt(x -> x.getHeight() + ENTRY_SPACING).sum();
        double maxHeight = ENTRIES_START_Y_OFFSET * 2 + componentHeight + 80;
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
        }

        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {  
        // Tooltips
        ModGuiUtils.renderTooltip(this, addButton, List.of(tooltipAdd.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, 0);
        ModGuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, 0);

        entriesCollection.performForEachOfType(IForegroundRendering.class, x -> {
            if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
                x.renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTicks);
            }
        });
        if (newTrainGroupWidget != null) {
            newTrainGroupWidget.renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		float scrollOffset = scroll.getValue();

        if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
            for (AbstractWidget w : entriesCollection.components) {
                if (w.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton)) {
                    return true;
                }
            }
            
            if (newTrainGroupWidget != null) {
                newTrainGroupWidget.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton);
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private float getScrollMax() {
        float max = -AREA_H + (ENTRIES_START_Y_OFFSET * 2) + 80;
        for (AbstractWidget w : entriesCollection.components) {
            max += 4 + w.getHeight();
        }
        return max;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
		float scrollOffset = scroll.getValue();

        if (entriesCollection.components.stream().anyMatch(x -> x.mouseScrolled(pMouseX, pMouseY + scrollOffset, pDelta))) {
            return true;
        }

		float chaseTarget = scroll.getChaseTarget();
		
        float max = getScrollMax();

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        entriesCollection.performForEach(x -> x.keyPressed(pKeyCode, pScanCode, pModifiers));
        if (newTrainGroupWidget != null) {
            newTrainGroupWidget.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        entriesCollection.performForEach(x -> x.charTyped(pCodePoint, pModifiers));
        if (newTrainGroupWidget != null) {
            newTrainGroupWidget.charTyped(pCodePoint, pModifiers);
        }
        return super.charTyped(pCodePoint, pModifiers);
    }
}
