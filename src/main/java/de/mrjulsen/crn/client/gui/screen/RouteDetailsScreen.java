package de.mrjulsen.crn.client.gui.screen;

import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.TrainIconType;
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
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlayScreen;
import de.mrjulsen.crn.client.gui.widgets.ExpandButton;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.event.listeners.IJourneyListenerClient;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.crn.util.ModTimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RouteDetailsScreen extends Screen implements IForegroundRendering, IJourneyListenerClient {

    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;
    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/route_details.png");    
    private static final int ON_TIME = 0x1AEA5F;
    private static final int DELAYED = 0xFF4242;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 18;
    private static final int ENTRY_WIDTH = 220;
    private static final int ENTRY_TIME_X = 28;
    private static final int ENTRY_DEST_X = 66;

    
    private final int AREA_X = 16;
    private final int AREA_Y = 53;
    private final int AREA_W = 220;
    private final int AREA_H = 157;

    private int guiLeft, guiTop;    
    private int scrollMax = 0;

    // Controls
    private IconButton backButton;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
    private final ExpandButton[] expandButtons;
    private final ControlCollection expandButtonCollection = new ControlCollection();

    // Data
    private final SimpleRoute route;
    private final Screen lastScreen;
    private final Font font;
    private final Font shadowlessFont;
    private final Level level;

    // Tooltips
    private final TranslatableComponent departureText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".route_details.departure");
    private final TranslatableComponent transferText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".route_details.transfer");
    private final TranslatableComponent timeNowText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".time.now");

    @SuppressWarnings("resource")
    public RouteDetailsScreen(Screen lastScreen, Level level, SimpleRoute route, UUID listenerId) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".route_details.title"));
        this.lastScreen = lastScreen;
        this.route = route;
        this.font = Minecraft.getInstance().font;  
        this.shadowlessFont = new NoShadowFontWrapper(font);    
        this.level = level;
        JourneyListenerManager.get(listenerId, this);

        int count = route.getParts().size();
        expandButtons = new ExpandButton[count];
        for (int i = 0; i < count; i++) {
            expandButtons[i] = new ExpandButton(font, 0, 0, false, (btn) -> {});
            expandButtonCollection.components.add(expandButtons[i]);
        }
    }

    @Override
    public UUID getJourneyListenerClientId() {
        return UUID.randomUUID();
    }

    public int getCurrentTime() {
        return (int)(level.getDayTime() % Constants.TICKS_PER_DAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);        
        JourneyListenerManager.removeClientListenerForAll(this);
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

        this.addRenderableWidget(new IconButton(guiLeft + 21 + DEFAULT_ICON_BUTTON_WIDTH + 4, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.BOOKMARK.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                HudOverlays.setOverlay(new RouteDetailsOverlayScreen(level, route));
            }
        });
    }

    @Override
    public void tick() {
		scroll.tickChaser();

        super.tick();
    }

    private int renderRouteStart(PoseStack poseStack, int x, int y, StationEntry stop) {
        final int HEIGHT = 30;
        final int V = 48;

        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(poseStack, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        int pY = y + 15;
        if (stop.renderRealtime() && stop.relatimeWasUpdated()) {
            pY -= (stop.renderRealtime() ? 5 : 0);
            drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY + 10, stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? DELAYED : ON_TIME);
        }
        drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)((route.getRefreshTime() + Constants.TIME_SHIFT) % 24000 + stop.getTicks()), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY, 0xFFFFFF);

        drawString(poseStack, shadowlessFont, stop.getStationName(), x + ENTRY_DEST_X, y + 15, 0xFFFFFF);
        drawString(poseStack, shadowlessFont, stop.getInfo().platform(), x + ENTRY_DEST_X + 129 - shadowlessFont.width(stop.getInfo().platform()), y + 15, 0xFFFFFF);

        return HEIGHT;
    }

    private int renderTrainDetails(PoseStack poseStack, int x, int y, SimpleRoutePart part) {
        final int HEIGHT = 43;
        final int V = 99;
        final float scale = 0.75f;
        final float mul = 1 / scale;

        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(poseStack, x, y, 0, V, ENTRY_WIDTH, HEIGHT);
        part.getTrainIcon().render(TrainIconType.ENGINE, poseStack, x + ENTRY_DEST_X, y + 7);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        drawString(poseStack, shadowlessFont, String.format("%s (%s)", part.getTrainName(), part.getTrainID().toString().split("-")[0]), (int)((x + ENTRY_DEST_X + 24) / scale), (int)((y + 7) / scale), 0xDBDBDB);
        drawString(poseStack, shadowlessFont, String.format("→ %s", part.getScheduleTitle()), (int)((x + ENTRY_DEST_X + 24) / scale), (int)((y + 17) / scale), 0xDBDBDB);
        poseStack.scale(mul, mul, mul);
        poseStack.popPose();

        return HEIGHT;
    }

    private int renderStop(PoseStack poseStack, int x, int y, StationEntry stop) {
        final int HEIGHT = 21;
        final int V = 78;

        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(poseStack, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        int pY = y + 6;
        if (stop.renderRealtime() && stop.relatimeWasUpdated()) {
            pY -= (stop.renderRealtime() ? 5 : 0);
            drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY + 10, stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? DELAYED : ON_TIME);
        }
        drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)((route.getRefreshTime() + Constants.TIME_SHIFT) % 24000 + stop.getTicks()), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY, 0xFFFFFF);
        drawString(poseStack, shadowlessFont, stop.getStationName(), x + ENTRY_DEST_X, y + 6, 0xFFFFFF);
        drawString(poseStack, shadowlessFont, stop.getInfo().platform(), x + ENTRY_DEST_X + 129 - shadowlessFont.width(stop.getInfo().platform()), y + 6, 0xFFFFFF);

        return HEIGHT;
    }

    private int renderRouteEnd(PoseStack poseStack, int x, int y, StationEntry stop) {
        final int HEIGHT = 44;
        final int V = 142;

        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(poseStack, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        int pY = y + 21;
        if (stop.renderRealtime() && stop.relatimeWasUpdated()) {
            pY -= (stop.renderRealtime() ? 5 : 0);
            drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY + 10, stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? DELAYED : ON_TIME);
        }
        drawString(poseStack, shadowlessFont, TimeUtils.parseTime((int)((route.getRefreshTime() + Constants.TIME_SHIFT) % 24000 + stop.getTicks()), TimeFormat.HOURS_24), x + ENTRY_TIME_X, pY, 0xFFFFFF);
        drawString(poseStack, shadowlessFont, stop.getStationName(), x + ENTRY_DEST_X, y + 21, 0xFFFFFF);
        drawString(poseStack, shadowlessFont, stop.getInfo().platform(), x + ENTRY_DEST_X + 129 - shadowlessFont.width(stop.getInfo().platform()), y + 21, 0xFFFFFF);

        return HEIGHT;
    }

    private int renderTransfer(PoseStack poseStack, int x, int y, int a, int b) {
        final int HEIGHT = 24;
        final int V = 186;

        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(poseStack, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        int time = -1;
        if (a < 0 || b < 0) {
            time = -1;
        } else {
            time = b - a;
        }

        drawString(poseStack, shadowlessFont, transferText.getString() + " " + (time < 0 ? "" : "(" + TimeUtils.parseDuration(time) + ")"), x + ENTRY_TIME_X, y + 8, 0xFFFFFF);

        return HEIGHT;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        for (Widget widget : this.renderables)
            widget.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);
        
        drawCenteredString(pPoseStack, font, departureText, guiLeft + GUI_WIDTH / 2, guiTop + 19, 0xFFFFFF);

        pPoseStack.pushPose();
        pPoseStack.scale(2, 2, 2);

        int departureTicks = route.getStartStation().getTicks();
        int departureTime = (int)(route.getRefreshTime() % 24000 + departureTicks);
        drawCenteredString(pPoseStack, font, departureTime - getCurrentTime() < 0 ? timeNowText.getString() : ModTimeUtils.parseTimeWithoutCorrection(departureTime - getCurrentTime()), (guiLeft + GUI_WIDTH / 2) / 2, (guiTop + 31) / 2, 0xFFFFFF);

        pPoseStack.scale(0.5f, 0.5f, 0.5f);
        pPoseStack.popPose();

        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        int yOffs = guiTop + 45 + ENTRIES_START_Y_OFFSET;
        SimpleRoutePart[] partsArray = route.getParts().toArray(SimpleRoutePart[]::new);
        for (int i = 0; i < partsArray.length; i++) {
            SimpleRoutePart part = partsArray[i];

            yOffs += renderRouteStart(pPoseStack, guiLeft + 16, yOffs, part.getStartStation());
            yOffs += renderTrainDetails(pPoseStack, guiLeft + 16, yOffs, part);

            ExpandButton btn = expandButtons[i];
            btn.active = part.getStopovers().size() > 0;

            if (btn.active) {
                btn.x = guiLeft + 78;
                btn.y = yOffs - 14;

                btn.render(pPoseStack, pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
            }

            if (btn.isExpanded()) {
                for (StationEntry stop : part.getStopovers()) {
                    yOffs += renderStop(pPoseStack, guiLeft + 16, yOffs, stop);
                }
            }

            yOffs += renderRouteEnd(pPoseStack, guiLeft + 16, yOffs, part.getEndStation());

            if (i < partsArray.length - 1) {
                int a = part.getEndStation().getTicks();
                int b = partsArray[i + 1].getStartStation().getTicks();
                yOffs += renderTransfer(pPoseStack, guiLeft + 16, yOffs, a, b);
            }
        }
        scrollMax = yOffs - guiTop - 45;
        pPoseStack.popPose();
        ModGuiUtils.endStencil();
        
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10,
        0x77000000, 0x00000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H,
        0x00000000, 0x77000000);

        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

        // Scrollbar
        double maxHeight = scrollMax;
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
        ModGuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, 0);        
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        
		float chaseTarget = scroll.getChaseTarget();
		float max = -AREA_H;
        max += scrollMax;

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		float scrollOffset = scroll.getValue();

        if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
            expandButtonCollection.performForEach(x -> x.active, x -> x.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton));
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
    
}
