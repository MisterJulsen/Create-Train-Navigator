package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.ISaveableNavigatorData;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.ISaveableNavigatorData.SaveableNavigatorDataLine;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem.ContextMenuItemData;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class SavedRouteWidget extends DLButton {    

    public static final int WIDTH = 180;
    public static final int HEADER_HEIGHT = 20;
    public static final int DEFAULT_LINE_HEIGHT = 12;
    public static final float DEFAULT_SCALE = 0.75f;

    private static final int DISPLAY_WIDTH = WIDTH - 20;
    
    private final ISaveableNavigatorData data;

    private final MutableComponent transferText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.transfer");
    private final MutableComponent connectionInPast = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    private final MutableComponent textShowDetails = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.show_details");
    private final MutableComponent textRemove = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.remove");
    private final MutableComponent textShare = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.share");
    private final MutableComponent textShowNotifications = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.notifications");

    public SavedRouteWidget(SavedRoutesViewer parent, int x, int y, ISaveableNavigatorData data) {
        super(x, y, WIDTH, 50, TextUtils.empty(), (b) -> clickAction(parent, data));
        this.data = data;
        set_height(HEADER_HEIGHT + 10 + data.getOverviewData().stream().mapToInt(a -> (int)(Math.max(DEFAULT_LINE_HEIGHT, ClientWrapper.getTextBlockHeight(font, a.text(), (int)(DISPLAY_WIDTH / DEFAULT_SCALE))) * DEFAULT_SCALE)).sum());

        setRenderStyle(AreaStyle.FLAT);
        setMenu(new DLContextMenu(() -> GuiAreaDefinition.of(this), () -> new DLContextMenuItem.Builder()
            .add(new ContextMenuItemData(textShowDetails, Sprite.empty(), true, (b) -> onPress.onPress(b), null))
            .addSeparator()
            .add(new ContextMenuItemData(textRemove, Sprite.empty(), true, (b) -> {
                SavedRoutesManager.removeRoute((ClientRoute)data);
                SavedRoutesManager.push(true, null);
                parent.displayRoutes(SavedRoutesManager.getAllSavedRoutes());
            }, null))
            //.add(new ContextMenuItemData(textShare, Sprite.empty(), true, (b) -> {}, null))
            .addSeparator()
            .add(new ContextMenuItemData(textShowNotifications, data instanceof ClientRoute route && route.shouldShowNotifications() ? GuiIcons.CHECKMARK.getAsSprite(8, 8) : Sprite.empty(), data instanceof Route, (b) -> {
                if (data instanceof ClientRoute route) {
                    route.setShowNotifications(!route.shouldShowNotifications());
                }
            }, null))
        ));
    }

    private static void clickAction(SavedRoutesViewer parent, ISaveableNavigatorData data) {
        if (data instanceof ClientRoute route) {
            Minecraft.getInstance().setScreen(new RouteDetailsScreen(parent.getParent(), route));
        }
    }    

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.DARK.getColor());
        CreateDynamicWidgets.renderHorizontalSeparator(graphics, x() + 6, y() + 16, width() - 12 - data.getTitle().icon().getWidth());

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0x22FFFFFF);
        }
        
        GuiUtils.drawString(graphics, font, x() + 6, y() + 5, data.getTitle().text(), 0xFFFFFFFF, EAlignment.LEFT, false);
        data.getTitle().icon().render(graphics, x() + width() - data.getTitle().icon().getWidth() - 3, y() + 3);
        
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(x() + 10, y() + HEADER_HEIGHT, 0);
        for (SaveableNavigatorDataLine line : data.getOverviewData()) {
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(DEFAULT_SCALE, DEFAULT_SCALE, 1);
            line.icon().render(graphics, 0, -2);
            int height = (int)(ClientWrapper.renderMultilineLabelSafe(graphics, (int)(16 / DEFAULT_SCALE), (int)(2 / DEFAULT_SCALE), font, line.text(), (int)(DISPLAY_WIDTH / DEFAULT_SCALE), 0xFFFFFFFF) * DEFAULT_SCALE);
            graphics.poseStack().popPose();
            graphics.poseStack().translate(0, Math.max((int)(DEFAULT_LINE_HEIGHT * DEFAULT_SCALE), height + 4), 0);
        }

        graphics.poseStack().popPose();
    }
    
}
