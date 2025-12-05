package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.windows.ScheduleBoardWindow;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;

public class RoutePartEntryWidget extends DLButton {
    
    protected static final DLTexture GUI = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png"), 256, 256);
    protected static final int ENTRY_WIDTH = 225;

    private final ClientRoute route;
    private final ClientRoutePart part;
    private final ClientTrainStop stop;
    private final TrainStopType type;
    private boolean valid;

    public RoutePartEntryWidget(ClientRoutePart part, ClientRoute route, ClientTrainStop stop, TrainStopType type, boolean valid) {
        super(0, 0, ENTRY_WIDTH, type.h);
        this.route = route;
        this.part = part;
        this.stop = stop;
        this.type = type;
        this.valid = valid;

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new ScheduleBoardWindow(mgr, stop.getRealTimeStationTag()));
            return false;
        });

    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawTexture(GUI, graphics, 0, 0, ENTRY_WIDTH, height(), 0, type.v, ENTRY_WIDTH, height(), TextureFillMode.STRETCH);
        renderData(graphics, type.dy);

        if (isSelected()) {
            GuiUtils.fill(graphics, 24, type.dy - 1, 199, 20, DLColor.fromInt(0x22FFFFFF));
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.valid = route.isPartReachable(part);
    }

    protected void renderData(DLGuiGraphics graphics, int y) {
        final float scale = 0.75f;

        String platformText = stop.getRealTimeStationTag().info().platform();
        String nameText = stop.getRealTimeStationTag().tagName();
        int maxStationNameWidth = 138 - 8 - graphics.defaultFont().width(platformText) - 6;

        GuiUtils.drawString(graphics, graphics.defaultFont(), 80, type.dy + 5, TextUtils.truncateWithEllipsis(graphics.defaultFont(), nameText, maxStationNameWidth), DLColor.WHITE, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), width() - 12, type.dy + 5, platformText, DLColor.WHITE, ETextAlignment.RIGHT, false);

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, 1);

        int precision = ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();

        if (this.type == TrainStopType.TRANSIT) {
            graphics.poseStack().translate((x() + 28) / scale, (y + 2) / scale, 0);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0,  0, TextUtils.text(DLTime.fromTicks(stop.getScheduledArrivalTime(),   VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? DLColor.WHITE : Constants.COLOR_DELAYED, ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 12, TextUtils.text(DLTime.fromTicks(stop.getScheduledDepartureTime(), VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? DLColor.WHITE : Constants.COLOR_DELAYED, ETextAlignment.LEFT, false);
            
            if (stop.shouldRenderRealTime() && !part.isCancelled() && valid) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), 30,  0, TextUtils.text(DLTime.fromTicks(stop.getScheduledArrivalTime() + (stop.getArrivalTimeDeviation() / precision * precision),     VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)), stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);        
                GuiUtils.drawString(graphics, graphics.defaultFont(), 30, 12, TextUtils.text(DLTime.fromTicks(stop.getScheduledDepartureTime() + (stop.getDepartureTimeDeviation() / precision * precision), VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)), stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);
            }
        } else {
            graphics.poseStack().translate((28) / scale, (y + 6) / scale, 0); 
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, TextUtils.text(DLTime.fromTicks((type == TrainStopType.START ? stop.getScheduledDepartureTime() : stop.getScheduledArrivalTime()), VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)).withStyle(valid ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), valid ? DLColor.WHITE : Constants.COLOR_DELAYED, ETextAlignment.LEFT, false);
            if (stop.shouldRenderRealTime() && !part.isCancelled() && valid) {
                long realTime = type == TrainStopType.START ? stop.getScheduledDepartureTime() + (stop.getDepartureTimeDeviation() / precision * precision) : stop.getScheduledArrivalTime() + (stop.getArrivalTimeDeviation() / precision * precision);
                GuiUtils.drawString(graphics, graphics.defaultFont(), 30, 0, TextUtils.text(DLTime.fromTicks(realTime, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)), (type == TrainStopType.START ? stop.isDepartureDelayed() : stop.isArrivalDelayed()) ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);        
            }            
        }

        graphics.poseStack().popPose();
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
