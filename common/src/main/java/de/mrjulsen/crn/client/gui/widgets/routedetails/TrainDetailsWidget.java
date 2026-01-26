package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.client.gui.windows.TrainJourneyWindow;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class TrainDetailsWidget extends DLGuiComponent {
    
    protected static final DLTexture GUI = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png"), 256, 256);
    protected static final int ENTRY_WIDTH = 225;
    protected static final int V = 92;
    
    private final RoutePartWidget container;
    private final ClientTrainStop stop;
    private final ClientRoutePart part;

    private final DLPanel statusInfoPanel;

    public TrainDetailsWidget(RoutePartWidget container, ClientRoute route, ClientRoutePart part, ClientTrainStop firstStop) {
        super(0, 0, ENTRY_WIDTH, 20);
        this.stop = firstStop;
        this.part = part;
        this.container = container;

        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(1, 9, 1, 76));
        layout.verticalGap.set(1);
        layout.wrap.set(false);
        this.layout.set(layout);

        addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {
            setHeight(e.layoutResult().contentHeight());
            return false;
        });

        addComponent(new TrainDataWidget(firstStop, part));

        DLButton showJourneyBtn = addComponent(new DLButton(0, 0, 1, 14));
        showJourneyBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        showJourneyBtn.text.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.title"));
        showJourneyBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new TrainJourneyWindow(mgr, route, part.getTrainId()));
            return false;
        });
        
        if (!part.getStopovers().isEmpty()) {
            DLButton showDetailsBtn = addComponent(new DLButton(0, 0, 1, 14));
            showDetailsBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
            showDetailsBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                container.expanded.toggle();
                updateShowDetailsBtn(showDetailsBtn);
                return false;
            });
            updateShowDetailsBtn(showDetailsBtn);
        }
        
        statusInfoPanel = new DLPanel(0, 0, 0, 0);
        FlowLayout statusLayout = new FlowLayout();
        statusLayout.fillCrossAxis.set(true);
        statusLayout.flowDirection.set(Direction.VERTICAL);
        statusLayout.wrap.set(false);
        statusInfoPanel.layout.set(statusLayout);
        addComponent(statusInfoPanel);

        statusInfoPanel.addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {
            statusInfoPanel.setHeight(e.layoutResult().contentHeight());
            return false;
        });
        
        part.listen(ClientRoutePart.EVENT_UPDATE, this, (data) -> {
            updateStatus();
        });
        
        updateStatus();
    }
    

    private void updateStatus() {
        statusInfoPanel.clearComponents();
        for (CompiledTrainStatus status : part.getStatus()) {
            statusInfoPanel.addComponent(new TrainStatusInfoWidget(0, 0, 0, status));
        }
    } 

    protected void updateShowDetailsBtn(DLButton showDetailsBtn) {        
        showDetailsBtn.text.set(container.expanded.get() ? Constants.TOOLTIP_COLLAPSE : Constants.TOOLTIP_EXPAND);
        showDetailsBtn.icon.set((container.expanded.get() ? GuiIcons.ARROW_UP : GuiIcons.ARROW_DOWN).getAsSprite(16, 16));
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawTexture(GUI, graphics, 0, 0, ENTRY_WIDTH, height(), 0, V, ENTRY_WIDTH, 1, TextureFillMode.STRETCH);
    }
    
}
