package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.screen.ScheduleBoardScreen;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class RoutePartEntryWidget extends DLButton {

    protected static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png");
    protected static final int GUI_TEXTURE_WIDTH = 256;
    protected static final int GUI_TEXTURE_HEIGHT = 256;
    protected static final int ENTRY_WIDTH = 225;

    private final ClientRoutePart part;
    private final ClientTrainStop stop;
    private final TrainStopType type;
    private boolean valid;

    public RoutePartEntryWidget(Screen parent, ClientRoutePart part, ClientTrainStop stop, int pX, int pY, int width, TrainStopType type, boolean valid) {
        super(pX, pY, width, type.h, TextUtils.empty(), (b) -> {
            DLScreen.setScreen(new ScheduleBoardScreen(parent, stop.getClientTag()));
        });
        this.part = part;
        this.stop = stop;
        this.type = type;
        this.valid = valid;
    }

    public void setValid(boolean b) {
        this.valid = b;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.drawTexture(GUI, graphics, x(), y(), ENTRY_WIDTH, height(), 0, type.v, ENTRY_WIDTH, height(), GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
        renderData(graphics, y() + type.dy);

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x() + 24, y() + type.dy - 1, 199, 20, 0x22FFFFFF);
        }
    }

    protected void renderData(Graphics graphics, int y) {
        final float scale = 0.75f;
        String platformText = stop.getClientTag().info().platform();
        String nameText = stop.getClientTag().tagName();
        int maxStationNameWidth = 138 - 8 - font.width(platformText) - 6;
        
        if (font.width(nameText) > maxStationNameWidth) {
            GuiUtils.drawString(graphics, font, x() + 80, y + 5, TextUtils.text(font.substrByWidth(TextUtils.text(stop.getClientTag().tagName()), maxStationNameWidth).getString()).append(Constants.ELLIPSIS_STRING), 0xFFFFFFFF, EAlignment.LEFT, false);
        } else {
            GuiUtils.drawString(graphics, font, x() + 80, y + 5, nameText, 0xFFFFFFFF, EAlignment.LEFT, false);
        }
        GuiUtils.drawString(graphics, font, x() + 213, y + 5, platformText, 0xFFFFFFFF, EAlignment.RIGHT, false);

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, 1);

        int precision = ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();

        if (this.type == TrainStopType.TRANSIT) {
            graphics.poseStack().translate((x() + 28) / scale, (y + 2) / scale, 0);
            GuiUtils.drawString(graphics, font, 00, 00, TextUtils.text(TimeUtils.parseTime(stop.getScheduledArrivalTime() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? 0xFFFFFFFF : Constants.COLOR_DELAYED, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 00, 12, TextUtils.text(TimeUtils.parseTime(stop.getScheduledDepartureTime() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? 0xFFFFFFFF : Constants.COLOR_DELAYED, EAlignment.LEFT, false);
            
            if (stop.shouldRenderRealTime() && !part.isCancelled() && valid) {
                GuiUtils.drawString(graphics, font, 30, 00, TimeUtils.parseTime(stop.getScheduledArrivalTime() + (stop.getArrivalTimeDeviation() / precision * precision) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()), stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);        
                GuiUtils.drawString(graphics, font, 30, 12, TimeUtils.parseTime(stop.getScheduledDepartureTime() + (stop.getDepartureTimeDeviation() / precision * precision) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()), stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
            }
        } else {
            graphics.poseStack().translate((x() + 28) / scale, (y + 6) / scale, 0);
            GuiUtils.drawString(graphics, font, 00, 00, TextUtils.text(TimeUtils.parseTime((type == TrainStopType.START ? stop.getScheduledDepartureTime() : stop.getScheduledArrivalTime()) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? 0xFFFFFFFF : Constants.COLOR_DELAYED, EAlignment.LEFT, false);
            if (stop.shouldRenderRealTime() && !part.isCancelled() && valid) {
                long realTime = type == TrainStopType.START ? stop.getScheduledDepartureTime() + (stop.getDepartureTimeDeviation() / precision * precision) : stop.getScheduledArrivalTime() + (stop.getArrivalTimeDeviation() / precision * precision);
                GuiUtils.drawString(graphics, font, 30, 00, TimeUtils.parseTime(realTime + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()), (type == TrainStopType.START ? stop.isDepartureDelayed() : stop.isArrivalDelayed()) ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);        
            }
        }

        graphics.poseStack().popPose();
        //GuiUtils.drawString(graphics, font, x(), y(), stop.getState().name() + ", Running: " + !stop.isClosed(), 0xFFFF0000, EAlignment.LEFT, false);
        //GuiUtils.drawString(graphics, font, x(), y() + height() - font.lineHeight, "" + stop.getScheduleIndex() + ": " + stop.getScheduledCycle() + " / " + stop.getSimulationTime() + " / " + stop.getScheduledArrivalTime() + " / " + stop.debug_test + (stop.isSimulated() ? "s" : ""), 0xFFFF0000, EAlignment.LEFT, false);
        //GuiUtils.drawString(graphics, font, x(), y() + height() - font.lineHeight, String.valueOf(stop.getTag().getId()), 0xFFFF0000, EAlignment.LEFT, false);
    }

    public static enum TrainStopType {
        START(48, 24, 4),
        TRANSIT(72, 21, 1),
        END(122, 33, 11);

        private int v;
        private int h;
        private int dy;

        TrainStopType(int v, int h, int dy) {
            this.v = v;
            this.h = h;
            this.dy = dy;
        }
    }
}
