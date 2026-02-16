package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.windows.RouteDetailsWindow;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.ISavableNavigatorData;
import de.mrjulsen.crn.data.ISavableNavigatorData.SavableNavigatorDataLine;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class SavedRouteWidget extends DLButton {    

    public static final int WIDTH = 180;
    public static final int HEADER_HEIGHT = 20;
    public static final int DEFAULT_LINE_HEIGHT = 12;
    public static final float DEFAULT_SCALE = 0.75f;

    private static final int DISPLAY_WIDTH = WIDTH - 20;
    
    private final ISavableNavigatorData data;

    private final MutableComponent transferText = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.transfer");
    private final MutableComponent connectionInPast = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    private final MutableComponent textShowDetails = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.show_details");
    private final MutableComponent textRemove = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.remove");
    private final MutableComponent textShare = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.share");
    private final MutableComponent textShowNotifications = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_route_widget.notifications");

    public SavedRouteWidget(SavedRoutesViewer parent, int x, int y, ISavableNavigatorData data) {
        super(x, y, WIDTH, 50);
        this.data = data;
        setHeight(HEADER_HEIGHT + 10 + data.getOverviewData().stream().mapToInt(a -> (int)(Math.max(DEFAULT_LINE_HEIGHT, ClientWrapper.getTextBlockHeight(Minecraft.getInstance().font, a.text(), (int)(DISPLAY_WIDTH / DEFAULT_SCALE))) * DEFAULT_SCALE)).sum());

        /*
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
        */

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (data instanceof ClientRoute route) {
                getWindowManager().createModal(mgr -> new RouteDetailsWindow(mgr, route));
            }
            return false;
        });
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK.getColor());
        CreateDynamicWidgets.renderHorizontalSeparator(graphics, 6, 16, width() - 12 - data.getTitle().icon().getWidth());

        if (isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x22FFFFFF));
        }
        
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 5, data.getTitle().text(), DLColor.WHITE, ETextAlignment.LEFT, false);
        data.getTitle().icon().render(graphics, width() - data.getTitle().icon().getWidth() - 3, 3);
        
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(10, HEADER_HEIGHT, 0);
        for (SavableNavigatorDataLine line : data.getOverviewData()) {
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(DEFAULT_SCALE, DEFAULT_SCALE, 1);
            line.icon().render(graphics, 0, -2);
            int height = (int)(ClientWrapper.renderMultilineLabelSafe(graphics, (int)(16 / DEFAULT_SCALE), (int)(2 / DEFAULT_SCALE), graphics.defaultFont(), line.text(), (int)(DISPLAY_WIDTH / DEFAULT_SCALE), DLColor.WHITE) * DEFAULT_SCALE);
            graphics.poseStack().popPose();
            graphics.poseStack().translate(0, Math.max((int)(DEFAULT_LINE_HEIGHT * DEFAULT_SCALE), height + 4), 0);
        }

        graphics.poseStack().popPose();
    }
    
}
