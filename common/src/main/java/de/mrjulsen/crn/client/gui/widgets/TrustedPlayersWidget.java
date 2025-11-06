package de.mrjulsen.crn.client.gui.widgets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.screen.GlobalSettingsScreen.IPlayerListSuggestionData;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLVerticalScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class TrustedPlayersWidget extends DLWidgetContainer {

    private final Screen parent;
    private final CRNListBox<Owner, Entry> listbox;
    private final Map<String, Owner> playerListByName = new HashMap<>();
    private final Set<Owner> currentPlayerList = new HashSet<>();

    private String searchText = "";

    public TrustedPlayersWidget(Screen parent, int x, int y, int width, int height, IPlayerListSuggestionData suggestions, Set<Owner> currentPlayerList) {
        super(x, y, width, height);
        this.parent = parent;
        this.currentPlayerList.addAll(currentPlayerList);

        GuiAreaDefinition listArea = new GuiAreaDefinition(x(), y() + 16, width() - 5, height() - 16 - 18);
        
        DLVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this.parent, listArea.getRight(), listArea.getY(), listArea.getHeight(), listArea);
        this.listbox = addRenderableWidget(new CRNListBox<>(this.parent, listArea.getX(), listArea.getY(), listArea.getWidth(), listArea.getHeight(), scrollBar));
        addRenderableWidget(scrollBar);
        DLEditBox addBox = addRenderableWidget(new DLCreateTextBox(font, x(), y() + height() - 18, width() - DLIconButton.DEFAULT_BUTTON_WIDTH, TextUtils.empty()));
        addBox.setResponder((value) -> {
            suggestions.run(addBox, this.playerListByName.values(), this.currentPlayerList);
        });
        DLIconButton addBtn = addRenderableWidget(new DLIconButton(
            ButtonType.DEFAULT,
            AreaStyle.FLAT,
            ModGuiIcons.ADD.getAsSprite(16, 16),
            x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH,
            y() + height() - DLIconButton.DEFAULT_BUTTON_HEIGHT,
            TextUtils.empty(),
            (btn) -> {
                if (addBox.getValue() != null && !addBox.getValue().isBlank() && this.playerListByName.containsKey(addBox.getValue())) {
                    this.currentPlayerList.add(this.playerListByName.get(addBox.getValue()));
                    addBox.setValue("");
                    reload();
                }
            })
        );
        addBtn.setBackColor(0);
        DLEditBox searchBox = addRenderableWidget(new DLEditBox(font, x() + 1, y() + 1, width() - 2, 14, TextUtils.empty()) {
            @Override
            public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
                if (code == GLFW.GLFW_KEY_ENTER) {
                    searchText = getValue();
                    refreshListBox();
                    return true;
                }
                return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
            }
        });
        searchBox.setValue(searchText);
        searchBox.withHint(DragonLib.TEXT_SEARCH);

        reload();
    }

    private void reload() {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ONLINE_PLAYERS, list -> {
            playerListByName.clear();
            for (Owner o : list) {
                playerListByName.put(o.name(), o);
            }
            refreshListBox();
        });
    }

    private void refreshListBox() {
        listbox.displayData(currentPlayerList.stream().filter(x -> x.name().toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))).sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList(), (player, i) -> {
            return new Entry(this, x(), y(), listbox.width(), player);
        });
    }

    public Set<Owner> getPlayers() {
        return currentPlayerList;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }

    private static class Entry extends DLWidgetContainer {

        private final Owner player;

        public Entry(TrustedPlayersWidget parent, int x, int y, int width, Owner player) {
            super(x, y, width, 18);
            this.player = player;

            DLIconButton deleteBtn = addRenderableWidget(new DLIconButton(
                ButtonType.DEFAULT,
                AreaStyle.FLAT,
                ModGuiIcons.DELETE.getAsSprite(16, 16),
                x() + width() - 16,
                y() + 1,
                16, 16,
                TextUtils.empty(),
                (btn) -> {
                    parent.currentPlayerList.removeIf(a -> a.equals(player));
                    parent.refreshListBox();
                })
            );
            deleteBtn.setBackColor(0);
        }

        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
            CreateDynamicWidgets.renderTextSlotOverlay(graphics, x(), y() + 1, width() - 18, height() - 2);
            GuiUtils.drawString(graphics, font, x() + 5, y() + 5, player.name(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, ETextAlignment.LEFT, false);
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.HOVERED;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) { }

        @Override
        public boolean consumeScrolling(double mouseX, double mouseY) {
            return false;
        }
        
    }
    
}
