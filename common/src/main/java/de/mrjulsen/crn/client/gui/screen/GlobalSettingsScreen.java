package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutColorPicker;
import de.mrjulsen.crn.client.gui.widgets.options.DLOptionsList;
import de.mrjulsen.crn.client.gui.widgets.options.DataListContainer;
import de.mrjulsen.crn.client.gui.widgets.options.OptionEntry;
import de.mrjulsen.crn.client.gui.widgets.options.SimpleDataListNewEntry;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLVerticalScrollBar;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GlobalSettingsScreen extends AbstractNavigatorScreen {
    
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private DLOptionsList viewer;
	private ModStationSuggestions destinationSuggestions;
    
    private final Component optionTagTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.title");
    private final Component optionTagDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.description");
    private final Component optionBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.title");
    private final Component optionBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.description");    
    private final Component optionTrainGroupTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_group.title");
    private final Component optionTrainGroupDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_group.description");    
    private final Component optionTrainBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.title");
    private final Component optionTrainBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.description");
    private final Component optionTrainLineTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.title");
    private final Component optionTrainLineDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.description");
    private final Component textAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.add");
    private final Component textColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.color");

    private final List<String> stationNames = new ArrayList<>();
    private final List<String> trainNames = new ArrayList<>();

    public GlobalSettingsScreen(Screen lastScreen) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.title"), BarColor.GRAY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
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
    protected void init() {
        super.init();
        setAllowedLayer(0);
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 8, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_GLOBAL_SETTINGS);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        int dy = FooterSize.DEFAULT.size() + 1;
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, guiLeft + GUI_WIDTH - 8, guiTop + dy, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, null);
        viewer = new DLOptionsList(this, guiLeft + 3, guiTop + dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, scrollBar);
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);
        
        viewer.addOption(null, optionTagTitle, optionTagDescription, (a, b) -> Minecraft.getInstance().setScreen(new StationTagSettingsScreen(this)), null);
        //viewer.addOption(null, optionTrainGroupTitle, optionTrainGroupDescription, (a, b) -> Minecraft.getInstance().setScreen(new TrainGroupScreen(this)), null);
        
        GlobalSettingsClient.getBlacklistedStations((datalist) -> {
            addBlacklistedStationsWidget(datalist.stream().sorted((a, b) -> a.compareToIgnoreCase(b)).toList(), scrollBar);
            GlobalSettingsClient.getBlacklistedTrains((datalist2) -> {
                addBlacklistedTrainsWidget(datalist2.stream().sorted((a, b) -> a.compareToIgnoreCase(b)).toList(), scrollBar);
                GlobalSettingsClient.getTrainGroups((datalist3) -> {
                    addTrainGroupsWidget(datalist3.stream().sorted((a, b) -> a.getGroupName().compareToIgnoreCase(b.getGroupName())).toList(), scrollBar);
                    GlobalSettingsClient.getTrainLines((datalist4) -> {
                        addTrainLinesWidget(datalist4.stream().sorted((a, b) -> a.getLineName().compareToIgnoreCase(b.getLineName())).toList(), scrollBar);
                    }); 
                });
            });
        });

        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_TRAIN_NAMES, (names) -> {
            this.trainNames.clear();
            this.trainNames.addAll(names);
            
            DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_NAMES, (names2) -> {
                this.stationNames.clear();
                this.stationNames.addAll(names2);
            });
        });

        //viewer.addOption(null, optionTrainBlacklistTitle, optionTrainBlacklistDescription, (a, b) -> OptionEntry.expandOrCollapse(a), null);
    }

    private void addBlacklistedStationsWidget(List<String> datalist, DLVerticalScrollBar scrollBar) {
        OptionEntry<?> opt = viewer.addOption((option) -> {
            GuiAreaDefinition workspace = option.getContentSpace();
            DataListContainer<Collection<String>, String> cont = new DataListContainer<>(option, workspace.getX(), workspace.getY(), workspace.getWidth(), datalist,
                (list) -> {
                    return list.iterator();
                }, (data, entryWidget) -> {
                    entryWidget.addDeleteButton((btn, tg, entry, refreshAction) -> {
                        GlobalSettingsClient.removeStationFromBlacklist(entry, (res) -> {
                            refreshAction.accept(Optional.ofNullable(res));
                        });
                    });
                    return data;
                }, (data, entryWidget) -> {
                    entryWidget.addAddButton(ModGuiIcons.ADD.getAsSprite(16, 16), textAdd,
                    (btn, tg, inputValues, refreshAction) -> {
                        String name = inputValues.get(SimpleDataListNewEntry.MAIN_INPUT_KEY).get();
                        if (name == null || name.isBlank()) {
                            return false;
                        }
                        GlobalSettingsClient.addStationToBlacklist(name,(res) -> {
                            refreshAction.accept(Optional.ofNullable(res));
                        });
                        return true;
                    });
                    entryWidget.editNameEditBox((box) -> {
                        box.setResponder((b) -> {
                            this.updateEditorSubwidgetsStations(box, data);
                        });
                        box.setMaxLength(StationTag.MAX_NAME_LENGTH);
                    });
                }, (self) -> {
                    option.notifyContentSizeChanged();
                }
            );
            cont.setPadding(3, 0, 3, 18);
            cont.setFilter((entry, searchText) -> {
                return entry.toLowerCase(Locale.ROOT).contains(searchText.get().toLowerCase(Locale.ROOT));
            });
            cont.setBordered(false);

            return cont;
        }, optionBlacklistTitle, optionBlacklistDescription, (a, b) -> OptionEntry.expandOrCollapse(a), null);
        opt.addAdditionalButton(ModGuiIcons.HELP.getAsSprite(16, 16), Constants.TEXT_HELP, (entry, btn) -> Util.getPlatform().openUri(Constants.HELP_PAGE_STATION_BLACKLIST));

    }

    private void addBlacklistedTrainsWidget(List<String> datalist, DLVerticalScrollBar scrollBar) {
        OptionEntry<?> opt = viewer.addOption((option) -> {
            GuiAreaDefinition workspace = option.getContentSpace();
            DataListContainer<Collection<String>, String> cont = new DataListContainer<>(option, workspace.getX(), workspace.getY(), workspace.getWidth(), datalist,
                (list) -> {
                    return list.iterator();
                }, (data, entryWidget) -> {
                    entryWidget.addDeleteButton((btn, tg, entry, refreshAction) -> {
                        GlobalSettingsClient.removeTrainFromBlacklist(entry, (res) -> {
                            refreshAction.accept(Optional.ofNullable(res));
                        });
                    });
                    return data;
                }, (data, entryWidget) -> {
                    entryWidget.addAddButton(ModGuiIcons.ADD.getAsSprite(16, 16), textAdd,
                    (btn, tg, inputValues, refreshAction) -> {
                        String name = inputValues.get(SimpleDataListNewEntry.MAIN_INPUT_KEY).get();
                        if (name == null || name.isBlank()) {
                            return false;
                        }
                        GlobalSettingsClient.addTrainToBlacklist(name, (res) -> {
                            refreshAction.accept(Optional.ofNullable(res));
                        });
                        return true;
                    });
                    entryWidget.editNameEditBox((box) -> {
                        box.setResponder((b) -> {
                            this.updateEditorSubwidgetsTrains(box, data);
                        });
                        box.setMaxLength(StationTag.MAX_NAME_LENGTH);
                    });
                }, (self) -> {
                    option.notifyContentSizeChanged();
                }
            );
            cont.setPadding(3, 0, 3, 18);
            cont.setFilter((entry, searchText) -> {
                return entry.toLowerCase(Locale.ROOT).contains(searchText.get().toLowerCase(Locale.ROOT));
            });
            cont.setBordered(false);

            return cont;
        }, optionTrainBlacklistTitle, optionTrainBlacklistDescription, (a, b) -> OptionEntry.expandOrCollapse(a), null);
        opt.addAdditionalButton(ModGuiIcons.HELP.getAsSprite(16, 16), Constants.TEXT_HELP, (entry, btn) -> Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_BLACKLIST));

    }

    private void addTrainGroupsWidget(List<TrainGroup> datalist, DLVerticalScrollBar scrollBar) {
        OptionEntry<?> opt = viewer.addOption((option) -> {
            GuiAreaDefinition workspace = option.getContentSpace();
            DataListContainer<Collection<TrainGroup>, TrainGroup> cont = new DataListContainer<>(option, workspace.getX(), workspace.getY(), workspace.getWidth(), datalist,
                (list) -> {
                    return list.iterator();
                }, (data, entryWidget) -> {
                    entryWidget.addDeleteButton((btn, tg, entry, refreshAction) -> {
                        GlobalSettingsClient.deleteTrainGroup(entry.getGroupName(), () -> {
                            GlobalSettingsClient.getTrainGroups((res) -> {
                                refreshAction.accept(Optional.ofNullable(res));
                            });
                        });
                    });
                    DLIconButton colorBtn = entryWidget.addButton(ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16), textColor,
                    (btn, tg, entry, refreshAction) -> {
                        final TrainGroup e = entry;
                        FlyoutColorPicker<?> flyout = new FlyoutColorPicker<>(this, e.getColor(), this::addRenderableWidget, (w) -> {
                            GlobalSettingsClient.updateTrainGroupColor(e.getGroupName(), ((FlyoutColorPicker<?>)w).getColorPicker().getSelectedColor(), () -> {
                                GlobalSettingsClient.getTrainGroups((res) -> {
                                    refreshAction.accept(Optional.ofNullable(res));
                                });
                            });
                            removeWidget(w);
                        });
                        flyout.setYOffset((int)-scrollBar.getScrollValue());
                        flyout.open(btn);
                    });
                    colorBtn.setBackColor(data.getColor());
                    return data.getGroupName();
                }, (data, entryWidget) -> {
                    entryWidget.addAddButton(ModGuiIcons.ADD.getAsSprite(16, 16), textAdd,
                    (btn, tg, inputValues, refreshAction) -> {
                        String name = inputValues.get(SimpleDataListNewEntry.MAIN_INPUT_KEY).get();
                        if (name == null || name.isBlank()) {
                            return false;
                        }
                        GlobalSettingsClient.createTrainGroup(name, (res) -> {
                            GlobalSettingsClient.getTrainGroups((r) -> {
                                refreshAction.accept(Optional.ofNullable(r));
                            });
                        });
                        return true;
                    });
                    entryWidget.editNameEditBox((box) -> {
                        box.setResponder((b) -> {
                            //this.updateEditorSubwidgetsTrains(box, data);
                        });
                        box.setMaxLength(StationTag.MAX_NAME_LENGTH);
                    });
                }, (self) -> {
                    option.notifyContentSizeChanged();
                }
            );
            cont.setPadding(3, 0, 3, 18);
            cont.setFilter((entry, searchText) -> {
                return entry.getGroupName().toLowerCase(Locale.ROOT).contains(searchText.get().toLowerCase(Locale.ROOT));
            });
            cont.setBordered(false);    
            return cont;
        }, optionTrainGroupTitle, optionTrainGroupDescription, (a, b) -> OptionEntry.expandOrCollapse(a), null);
        opt.addAdditionalButton(ModGuiIcons.HELP.getAsSprite(16, 16), Constants.TEXT_HELP, (entry, btn) -> Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_GROUPS));

    }

    private void addTrainLinesWidget(List<TrainLine> datalist, DLVerticalScrollBar scrollBar) {
        OptionEntry<?> opt = viewer.addOption((option) -> {
            GuiAreaDefinition workspace = option.getContentSpace();
            DataListContainer<Collection<TrainLine>, TrainLine> cont = new DataListContainer<>(option, workspace.getX(), workspace.getY(), workspace.getWidth(), datalist,
                (list) -> {
                    return list.iterator();
                }, (data, entryWidget) -> {
                    entryWidget.addDeleteButton((btn, tg, entry, refreshAction) -> {
                        GlobalSettingsClient.deleteTrainLine(entry.getLineName(), () -> {
                            GlobalSettingsClient.getTrainLines((res) -> {
                                refreshAction.accept(Optional.ofNullable(res));
                            });
                        });
                    });
                    DLIconButton colorBtn = entryWidget.addButton(ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16), textColor,
                    (btn, tg, entry, refreshAction) -> {
                        final TrainLine e = entry;
                        FlyoutColorPicker<?> flyout = new FlyoutColorPicker<>(this, e.getColor(), this::addRenderableWidget, (w) -> {
                            GlobalSettingsClient.updateTrainLineColor(e.getLineName(), ((FlyoutColorPicker<?>)w).getColorPicker().getSelectedColor(), () -> {
                                GlobalSettingsClient.getTrainLines((res) -> {
                                    refreshAction.accept(Optional.ofNullable(res));
                                });
                            });
                            removeWidget(w);
                        });
                        flyout.setYOffset((int)-scrollBar.getScrollValue());
                        flyout.open(btn);
                    });
                    colorBtn.setBackColor(data.getColor());
                    return data.getLineName();
                }, (data, entryWidget) -> {
                    entryWidget.addAddButton(ModGuiIcons.ADD.getAsSprite(16, 16), textAdd,
                    (btn, tg, inputValues, refreshAction) -> {
                        String name = inputValues.get(SimpleDataListNewEntry.MAIN_INPUT_KEY).get();
                        if (name == null || name.isBlank()) {
                            return false;
                        }
                        GlobalSettingsClient.createTrainLine(name, (res) -> {
                            GlobalSettingsClient.getTrainLines((r) -> {
                                refreshAction.accept(Optional.ofNullable(r));
                            });
                        });
                        return true;
                    });
                    entryWidget.editNameEditBox((box) -> {
                        box.setResponder((b) -> {
                            //this.updateEditorSubwidgetsTrains(box, data);
                        });
                        box.setMaxLength(StationTag.MAX_NAME_LENGTH);
                    });
                }, (self) -> {
                    option.notifyContentSizeChanged();
                }
            );
            cont.setPadding(3, 0, 3, 18);
            cont.setFilter((entry, searchText) -> {
                return entry.getLineName().toLowerCase(Locale.ROOT).contains(searchText.get().toLowerCase(Locale.ROOT));
            });
            cont.setBordered(false);    
            return cont;
        }, optionTrainLineTitle, optionTrainLineDescription, (a, b) -> OptionEntry.expandOrCollapse(a), null);
        opt.addAdditionalButton(ModGuiIcons.HELP.getAsSprite(16, 16), Constants.TEXT_HELP, (entry, btn) -> Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_LINES));
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


    public void updateEditorSubwidgetsTrains(DLEditBox field, Collection<String> blacklisted) {
        updateEditorSubwidgetsInternal(field, getViableTrains(trainNames, blacklisted));
	}

    public void updateEditorSubwidgetsStations(DLEditBox field, Collection<String> blacklisted) {
        updateEditorSubwidgetsInternal(field, getViableStations(stationNames, blacklisted));
	}

    private void updateEditorSubwidgetsInternal(DLEditBox field, List<String> data) {        
        clearSuggestions();
		destinationSuggestions = new ModStationSuggestions(Minecraft.getInstance(), this, field, font, data, field.getHeight() + 2 + field.y());
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
    }
    

    private List<String> getViableStations(Collection<String> src, Collection<String> blacklisted) {
        return src.stream()
            .distinct()
            .filter(x -> !blacklisted.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}

    private List<String> getViableTrains(Collection<String> src, Collection<String> blacklisted) {
        return src.stream()
            .distinct()
            .filter(x -> !blacklisted.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}
}
