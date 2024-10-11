package de.mrjulsen.crn.client.gui.widgets.routedetails;

import java.io.Closeable;
import java.util.Set;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.screen.TrainJourneySreen;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartWidget.RoutePartDetailsActionBuilder;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.RouteDetailsActionsEvent;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.DLWidgetsCollection;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Single;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RoutePartTrainDetailsWidget extends WidgetContainer implements Closeable {

    protected static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png");
    protected static final int GUI_TEXTURE_WIDTH = 256;
    protected static final int GUI_TEXTURE_HEIGHT = 256;
    protected static final int ENTRY_WIDTH = 225;
    protected static final int DEFAULT_HEIGHT = 28;
    protected static final int REASON_WIDTH = RoutePartWidget.ACTION_BTN_WIDTH - 8;
    protected static final int V = 92;

    private final ClientTrainStop stop;
    private final ClientRoutePart part;
    private Set<CompiledTrainStatus> status = Set.of();

    public static final int ACTION_BTN_WIDTH = 140;
    public static final int ACTION_BTN_HEIGHT = 14;
    private int actionIndex;
    private int currentHeight;
    private final DLWidgetsCollection actionButtons = new DLWidgetsCollection();
    private final RoutePartWidget container;

    public RoutePartTrainDetailsWidget(Screen parent, RoutePartWidget container, ClientRoute route, ClientRoutePart part, ClientTrainStop firstStop, int pX, int pY, int width) {
        super(pX, pY, width, DEFAULT_HEIGHT);
        this.stop = firstStop;
        this.part = part;
        this.container = container;

        part.listen(ClientRoutePart.EVENT_UPDATE, this, (data) -> {
            int oldHeight = currentHeight;
            currentHeight = DEFAULT_HEIGHT;
            updateStatus();
            currentHeight += actionIndex * (ACTION_BTN_HEIGHT + 1);
            int diff = currentHeight - oldHeight;
            actionButtons.performForEach(x -> x.set_y(x.y() + diff));
            set_height(currentHeight);
        });

        currentHeight = DEFAULT_HEIGHT;
        updateStatus();

        addAction(new RoutePartDetailsActionBuilder(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.title"), Sprite.empty(), (b) -> Minecraft.getInstance().setScreen(new TrainJourneySreen(parent, route, part.getTrainId()))));
        CRNEventsManager.getEventOptional(RouteDetailsActionsEvent.class).ifPresent(x -> x.run(route, part, container.isExpanded()).forEach(this::addAction));
        if (!part.getStopovers().isEmpty()) {
            addAction(new RoutePartDetailsActionBuilder(container.isExpanded() ? Constants.TOOLTIP_COLLAPSE : Constants.TOOLTIP_EXPAND, (container.isExpanded() ? GuiIcons.ARROW_UP : GuiIcons.ARROW_DOWN).getAsSprite(16, 16), (b) -> container.setExpanded(!container.isExpanded())));
        }
        
        currentHeight += actionIndex * (ACTION_BTN_HEIGHT + 1);
        set_height(currentHeight);
    }

    private void updateStatus() {        
        this.status = part.getStatus();
        currentHeight += status.stream().mapToInt(x -> Math.max(9, (int)(ClientWrapper.getTextBlockHeight(font, x.text(), (int)(REASON_WIDTH / 0.75f)) * 0.75f) + 2)).sum() /* TODO */ + 4;
    }    
    
    public void addAction(RoutePartDetailsActionBuilder builder) {
        DLIconButton btn2 = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, builder.icon(), x() + 76, y() + currentHeight + (actionIndex * (ACTION_BTN_HEIGHT + 1)), ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT, builder.text(), builder.onClick()) {
            @Override
            public void mouseMoved(double mouseX, double mouseY) {
                setFontColor(isInBounds(mouseX, mouseY) ? DragonLib.NATIVE_BUTTON_FONT_COLOR_HIGHLIGHT : DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE);
                super.mouseMoved(mouseX, mouseY);
            }
        });
        btn2.setFontColor(DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE);
        btn2.setBackColor(0x00000000);
        actionButtons.add(btn2);
        actionIndex++;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.drawTexture(GUI, graphics, x(), y(), ENTRY_WIDTH, DEFAULT_HEIGHT, 0, V, ENTRY_WIDTH, DEFAULT_HEIGHT, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
        renderData(graphics, y() + 1);
        super.renderMainLayer(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderData(Graphics graphics, int y) {
        final float scale = 0.75f;
        final float mul = 1 / scale;
        final float maxWidth = 140;

        //GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, DEFAULT_HEIGHT);
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y + DEFAULT_HEIGHT - 1, ENTRY_WIDTH, height() - DEFAULT_HEIGHT, 0, V + DEFAULT_HEIGHT, ENTRY_WIDTH, 1, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
        stop.getTrainIcon().render(TrainIconType.ENGINE, graphics.poseStack(), x + 80, y + 7);

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        Component trainName = TextUtils.text(part.getLastStop().getTrainDisplayName()).withStyle(ChatFormatting.BOLD);
        CreateDynamicWidgets.renderTextHighlighted(graphics, (int)((x() + 80 + 24) / scale), (int)((y + 4) / scale), font, trainName, part.getLastStop().getTrainDisplayColor());
        GuiUtils.drawString(graphics, font, (int)((x() + 80 + 24) / scale) + font.width(trainName) + 10, (int)((y + 6) / scale), GuiUtils.ellipsisString(font, TextUtils.text(String.format("%s (%s)", stop.getTrainName(), stop.getTrainId().toString().split("-")[0])), (int)((maxWidth - font.width(trainName) - 15) / scale)), 0xFFDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, (int)((x() + 80 + 24) / scale), (int)((y + 18) / scale), GuiUtils.ellipsisString(font, TextUtils.text(stop.getDisplayTitle()), (int)((maxWidth - 24) / scale)), 0xFFDBDBDB, EAlignment.LEFT, false);        
        graphics.poseStack().scale(mul, mul, mul);
        graphics.poseStack().popPose();
        
        // render reasons
        int reasonsY = 0;
        for (CompiledTrainStatus trainInformation : status) {
            reasonsY += trainInformation.render(graphics, Single.of(font), x() + 76, y() + DEFAULT_HEIGHT + 2 + reasonsY, REASON_WIDTH);
        }
    }

    @Override
    public void set_height(int h) {
        super.set_height(h);
        container.updateHeight();
    }

    public static enum TrainStopType {
        START(48, 30, 8),
        TRANSIT(78, 21, 1),
        END(142, 44, 14);

        private int v;
        private int h;
        private int dy;

        TrainStopType(int v, int h, int dy) {
            this.v = v;
            this.h = h;
            this.dy = dy;
        }

        public int getV() {
            return v;
        }

        public int getH() {
            return h;
        }

        public int getDy() {
            return dy;
        }        
    }

    @Override
    public void close() {
        part.stopListeningAll(this);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
}
