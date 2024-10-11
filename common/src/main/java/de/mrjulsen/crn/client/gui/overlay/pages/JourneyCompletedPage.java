package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;

public class JourneyCompletedPage extends AbstractRouteDetailsPage {

    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyCompleted = "gui.createrailwaysnavigator.route_overview.journey_completed";

    private static final int MAX_TICK_TIME = 200;
    private int ticks;
    private final Runnable after;
    
    private MultiLineLabel messageLabel;

    public JourneyCompletedPage(ClientRoute route, Runnable after) {
        super(route);
        this.messageLabel = MultiLineLabel.create(font, TextUtils.translate(keyAfterJourney, route.getEnd().getClientTag().tagName()), width() - 10);
        this.after = after;
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (ticks == MAX_TICK_TIME) {
            DLUtils.doIfNotNull(after, x -> x.run());
        }
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        int y = 3;
        // Title
        ModGuiIcons.CHECK.render(graphics, 5, y);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, TextUtils.translate(keyJourneyCompleted).withStyle(ChatFormatting.BOLD), Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.poseStack(), 10, y, font.lineHeight, 0xFFDBDBDB);
    }
    
}
