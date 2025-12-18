package de.mrjulsen.crn.client.gui.widgets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.SearchBox.SearchBoxRenderer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class TrustedPlayerListComponent extends DLGuiComponent {

    private final DLPanel contentPanel;
    private final SearchBox searchBox;
    private final DLScrollBar scrollBar;

    private final Map<String, Owner> playerPool = new HashMap<>();
    private final Set<Owner> players = new HashSet<>();

    public TrustedPlayerListComponent(int x, int y, int w, int h, Set<Owner> players) {
        super(x, y, w, h);
        this.players.addAll(players);
        contentPanel = addComponent(new DLPanel(0, FlatIconButton.HEIGHT, width(), height() - FlatIconButton.HEIGHT * 2));
        
        searchBox = addComponent(new SearchBox(0, 0, width()));
        this.searchBox.acceptAndCancelKeysEnabled.set(true);
        this.searchBox.placeholderText.set(Constants.TEXT_SEARCH);
        searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            refresh();
            return false;
        });

        FlatIconButton addBtn = addComponent(new FlatIconButton(width() - FlatIconButton.WIDTH, height() - FlatIconButton.HEIGHT, ModGuiIcons.ADD.getAsSprite(16, 16)));
        addBtn.anchor.set2(EAlign.TOP, EAlign.RIGHT);

        CreateTextBox inputBox = addComponent(new CreateTextBox(0, height() - FlatIconButton.HEIGHT, width() - FlatIconButton.WIDTH));        
        inputBox.acceptAndCancelKeysEnabled.set(true);
        inputBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            if (playerPool.containsKey(inputBox.text.get().getPlainText())) {
                this.players.add(playerPool.get(inputBox.text.get().getPlainText()));
                inputBox.text.get().clear();
                refresh();
            }
            return false;
        });
        
        scrollBar = addComponent(new DLScrollBar(contentPanel.x() + contentPanel.width() - 5, contentPanel.y(), 5, contentPanel.height(), Orientation.VERTICAL));
        scrollBar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollBar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
        scrollBar.scrollerSize.set(0);
        scrollBar.screenSize.set(contentPanel.height());
        scrollBar.scrollSteps.set(10);
        scrollBar.max.set(0);
        scrollBar.inputConsumptionPolicy.set((type) -> true);
        scrollBar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            contentPanel.setScrollOffsetY(e.value());
            return false;
        });
        
        addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (playerPool.containsKey(inputBox.text.get().getPlainText())) {
                this.players.add(playerPool.get(inputBox.text.get().getPlainText()));
                inputBox.text.get().clear();
                refresh();
            }
            return false;
        });

        reload();
        refresh();
    }

    public Set<Owner> getSelectedPlayers() {
        return players;
    }

    public int refresh() {
        contentPanel.clearComponents();
        int y = 0;
        for (Owner player : players) {
            if (!player.name().toLowerCase().contains(searchBox.text.get().getPlainText().toLowerCase())) {
                continue;
            }
            TrustedPlayerListItem item = contentPanel.addComponent(new TrustedPlayerListItem(this, player));
            item.setWidth(contentPanel.width());
            y += item.height();
        }
        scrollBar.max.set(y);
        return y;
    }
    
    private void reload() {
        ModNetworkManager.GET_ONLINE_PLAYERS.send(NetworkDirection.toServer(), (response) -> {
            playerPool.clear();
            for (Owner o : response.getPlayers()) {
                playerPool.put(o.name(), o);
            }            
        }, () -> {});
    }



    public static class TrustedPlayerListItem extends DLGuiComponent {

        private final Owner item;

        protected TrustedPlayerListItem(TrustedPlayerListComponent list, Owner item) {
            super(0, 0, 100, FlatIconButton.HEIGHT + 2);
            this.item = item;
            FlatIconButton deleteBtn = addComponent(new FlatIconButton(width() - FlatIconButton.WIDTH - 1, 1, ModGuiIcons.DELETE.getAsSprite(16, 16)));
            deleteBtn.anchor.set2(EAlign.RIGHT);
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                list.players.remove(item);
                list.refresh();
                return false;
            });
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            CreateDynamicWidgets.renderTextSlotOverlay(graphics, 1, 1, width() - FlatIconButton.WIDTH - 4, height() - 2);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 6, height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.text(item.name()), width() - FlatIconButton.WIDTH - 4 - 10), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        }
        
    }
    
}
