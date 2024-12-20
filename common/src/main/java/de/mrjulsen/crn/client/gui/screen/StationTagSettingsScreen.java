package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.options.DLOptionsList;
import de.mrjulsen.crn.client.gui.widgets.options.DataListContainer;
import de.mrjulsen.crn.client.gui.widgets.options.NewEntryWidget;
import de.mrjulsen.crn.client.gui.widgets.options.OptionEntry;
import de.mrjulsen.crn.client.gui.widgets.options.SimpleDataListNewEntry;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;

public class StationTagSettingsScreen extends AbstractNavigatorScreen {

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private DLOptionsList viewer;
    private DLEditBox searchBox;

    private final MutableComponent tooltipDeleteTag = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.delete_alias.tooltip");
    private final MutableComponent textAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.add");
    private final MutableComponent textStationName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.hint.station_name");
    private final MutableComponent textPlatformName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.hint.platform");
    
	private ModStationSuggestions destinationSuggestions;
    private StationTag selectedTag;
    private String searchText = "";

    private final List<String> stationNames = new ArrayList<>();

    public StationTagSettingsScreen(Screen lastScreen) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.title"), BarColor.GRAY);
    }

    @Override
    public void tick() {
        super.tick();

        DLUtils.doIfNotNull(destinationSuggestions, x -> {            
            x.tick();
            if (!destinationSuggestions.getEditBox().canConsumeInput()) {
                clearSuggestions();
            }
        });
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    public void reload() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        super.init();        
        
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_NAMES, (names) -> {
            this.stationNames.clear();
            this.stationNames.addAll(names);
        });
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 8, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_STATION_TAGS);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        int dy = FooterSize.DEFAULT.size() + 1;

        searchBox = addRenderableWidget(new DLEditBox(font, guiLeft + 4, guiTop + dy + 1, GUI_WIDTH - 8, 16, TextUtils.empty()) {
            @Override
            public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
                if (code == GLFW.GLFW_KEY_ENTER) {
                    searchText = getValue();
                    reload();
                    return true;
                }
                return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
            }
        });
        searchBox.setValue(searchText);
        searchBox.withHint(DragonLib.TEXT_SEARCH);
        dy += 18;

        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, guiLeft + GUI_WIDTH - 8, guiTop + dy, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, null);
        viewer = new DLOptionsList(this, guiLeft + 3, guiTop + dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, scrollBar);
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);

        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_TAGS, (tgs) -> {
            List<StationTag> tags = tgs.stream().sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList();
            for (StationTag tag : tags) {
                if (!tag.getTagName().get().toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                final StationTag stationTag = tag;
                OptionEntry<DataListContainer<StationTag, Map.Entry<String, StationInfo>>> opt = viewer.addOption((option) -> {
                    GuiAreaDefinition workspace = option.getContentSpace();

                    DataListContainer<StationTag, Map.Entry<String, StationInfo>> cont = new DataListContainer<>(option, workspace.getX(), workspace.getY(), workspace.getWidth(), stationTag,
                        (tg) -> {
                            return tg.getAllStations().entrySet().stream().sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey())).iterator();
                        }, (data, entryWidget) -> {
                            entryWidget.addDeleteButton((btn, tg, entry, refreshAction) -> {
                                GlobalSettingsClient.removeStationTagEntry(tag.getId(), entry.getKey(),
                                (newTag) -> {
                                    newTag.ifPresent(a -> refreshAction.accept(newTag));
                                });
                            });
                            entryWidget.addDataSection(40, (entry) -> entry.getValue().platform(), EAlignment.RIGHT,
                            (tg, entry, newValue, refreshAction) -> {
                                if (!newValue.isBlank() && !entry.getValue().platform().equals(newValue)) {
                                    GlobalSettingsClient.updateStationTagEntry(tg.getId(), entry.getKey(), new StationInfo(newValue),
                                    (newTag) -> {
                                        newTag.ifPresent(a -> refreshAction.accept(newTag));
                                    });
                                }
                            });
                            return data.getKey();
                        }, (data, entryWidget) -> {
                            entryWidget.addAddButton(ModGuiIcons.ADD.getAsSprite(16, 16), textAdd,
                            (btn, tg, inputValues, refreshAction) -> {
                                String name = inputValues.get(SimpleDataListNewEntry.MAIN_INPUT_KEY).get();
                                String platform = inputValues.get("platform").get();
                                if (name == null || platform == null || name.isBlank() || platform.isBlank()) {
                                    return false;
                                }
                                GlobalSettingsClient.addStationTagEntry(tg.getId(), name, new StationInfo(platform),
                                (newTag) -> {
                                    newTag.ifPresent(a -> refreshAction.accept(newTag));
                                });
                                return true;
                            });
                            entryWidget.editNameEditBox((box) -> {
                                box.setResponder((b) -> {
                                    this.updateEditorSubwidgets(box, data);
                                });
                                box.setMaxLength(StationTag.MAX_NAME_LENGTH);
                            });
                            entryWidget.setNameEditBoxTooltip((box) -> textStationName);
                            entryWidget.addDataSection(40, "platform", textPlatformName, (box) -> box.setMaxLength(StationInfo.MAX_PLATFORM_NAME_LENGTH));
                        }, (self) -> {
                            option.notifyContentSizeChanged();
                        }
                    );
                    cont.setPadding(3, 0, 3, 18);
                    cont.setFilter((entry, searchText) -> {
                        return entry.getKey().toLowerCase(Locale.ROOT).contains(searchText.get().toLowerCase(Locale.ROOT));
                    });
                    cont.setBordered(false);

                    return cont;
                }, TextUtils.text(tag.getTagName().get()), TextUtils.empty(), (a, b) -> OptionEntry.expandOrCollapse(a),
                (str) -> {
                    if (!str.isBlank()) {
                        GlobalSettingsClient.updateStationTagNameData(stationTag.getId(), str, () -> {});
                        return true;
                    }
                    return false;
                });
                opt.addAdditionalButton(ModGuiIcons.DELETE.getAsSprite(16, 16), tooltipDeleteTag,
                (entry, btn) -> {
                    GlobalSettingsClient.deleteStationTag(entry.getContentContainer().getData().getId(), () -> {
                        reload();
                    });
                });

                opt.setTooltip(List.of(
                    TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.summary", stationTag.getAllStationNames().size()),
                    TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.editor", stationTag.getLastEditorName(), stationTag.getLastEditedTimeFormatted())
                ));
            }

            viewer.addRenderableWidget(new NewEntryWidget(this, () -> Pair.of(-viewer.getXScrollOffset(), -viewer.getYScrollOffset()), (val) -> {
                GlobalSettingsClient.createStationTag(val, (tag) -> {
                    reload();
                });
                return true;
            }, 0, 0, viewer.getContentWidth()));

        });
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderNavigatorBackground(graphics, mouseX, mouseY, partialTicks);        
        
        int y = FooterSize.DEFAULT.size() - 1;
        int h = GUI_HEIGHT - y - FooterSize.SMALL.size();
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, h + 1, ContainerColor.PURPLE);

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTick);
                
        DLUtils.doIfNotNull(destinationSuggestions, x -> {   
            graphics.poseStack().pushPose();         
            graphics.poseStack().translate(-viewer.getXScrollOffset(), -viewer.getYScrollOffset(), 0);
            x.render(graphics.graphics(), (int)(mouseX + viewer.getXScrollOffset()), (int)(mouseY + viewer.getYScrollOffset()));
            graphics.poseStack().popPose();
        });
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(mouseX + viewer.getXScrollOffset(), mouseY + viewer.getYScrollOffset(), MathUtils.clamp(delta, -1.0D, 1.0D)))
			return true;

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (destinationSuggestions != null && destinationSuggestions.mouseClicked(mouseX + viewer.getXScrollOffset(), mouseY + viewer.getYScrollOffset(), button))
            return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }


    public void updateEditorSubwidgets(EditBox field, StationTag tag) {
        clearSuggestions();
        this.selectedTag = tag;
        
        destinationSuggestions = new ModStationSuggestions(Minecraft.getInstance(), this, field, font, getViableStations(stationNames, field), field.getHeight() + 2 + field.getY());
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableStations(Collection<String> src, EditBox field) {
        return src.stream()
            .distinct()
            .filter(x -> !selectedTag.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}
}
