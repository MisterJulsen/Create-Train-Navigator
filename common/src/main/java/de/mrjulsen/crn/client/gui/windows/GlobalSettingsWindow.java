package de.mrjulsen.crn.client.gui.windows;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.FlyoutColorPickerWidget;
import de.mrjulsen.crn.client.gui.flyout.FlyoutConfirmDialog;
import de.mrjulsen.crn.client.gui.flyout.FlyoutTrustedPlayersWidget;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.TrainAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.options.OptionEntry;
import de.mrjulsen.crn.client.gui.widgets.options.OptionEntryHeader;
import de.mrjulsen.crn.client.gui.widgets.options.OptionsDataView;
import de.mrjulsen.crn.client.gui.widgets.options.OptionsView;
import de.mrjulsen.crn.client.gui.widgets.options.TextOptionLabel;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.crn.network.packets.pain.AddStationToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.AddTrainToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveTrainFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainCategoryUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLinePermissionsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.Lock;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLEditableLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout.ColumnSizeMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.DataSlot;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.DataSlotComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.SizeMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class GlobalSettingsWindow extends AbstractNavigatorScreen {

    private final Component optionTagTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.title");
    private final Component optionTagDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.description").withStyle(ChatFormatting.GRAY);
    private final Component optionBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.title");
    private final Component optionBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.description").withStyle(ChatFormatting.GRAY);   
    private final Component optionTrainCategoryTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_category.title");
    private final Component optionTrainCategoryDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_category.description").withStyle(ChatFormatting.GRAY);   
    private final Component optionTrainBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.title");
    private final Component optionTrainBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.description").withStyle(ChatFormatting.GRAY);
    private final Component optionTrainLineTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.title");
    private final Component optionTrainLineDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.description").withStyle(ChatFormatting.GRAY);
    private final Component textColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_line.color");

    private final OptionsView optionsView;

    public GlobalSettingsWindow(DLWindowManager manager) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.title").append(GlobalSettingsClient.modificationsAllowed() ? TextUtils.empty() : TextUtils.text(" ").append(Constants.TEXT_READ_ONLY).withStyle(ChatFormatting.DARK_RED)), ContainerColor.PURPLE, BarColor.GRAY);
        manager.setPauseScreen(false);
        
        CreateButton helpButton = addComponent(new CreateButton(width() - CreateButton.WIDTH - 8, height() - CreateButton.HEIGHT - 6, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> { 
            Util.getPlatform().openUri(Constants.HELP_PAGE_GLOBAL_SETTINGS);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));

        optionsView = addComponent(new OptionsView(3, FooterSize.DEFAULT.size() + 1, GUI_WIDTH - 6, GUI_HEIGHT - (FooterSize.DEFAULT.size() + 1) - FooterSize.SMALL.size() - 1));

        createStationTagOption();
        createStationBlacklistOption();
        createTrainBlacklistOption();
        createTrainCategoryOption();
        createTrainLineOption();
    }

    private void reloadBlacklistedTrains(OptionsDataView<String> dataView) {
        ModNetworkManager.GET_ALL_BLACKLISTED_TRAINS.send(NetworkDirection.toServer(), (response) -> {
            dataView.items.setAll(response.getNames().stream().sorted((a, b) -> a.compareToIgnoreCase(b)).toList());
        }, () -> {});
    }

    private void reloadBlacklistedStations(OptionsDataView<String> dataView) {
        ModNetworkManager.GET_ALL_BLACKLISTED_STATIONS.send(NetworkDirection.toServer(), (response) -> {
            dataView.items.setAll(response.getNames().stream().sorted((a, b) -> a.compareToIgnoreCase(b)).toList());
        }, () -> {});
    }

    private void reloadTrainLines(OptionsDataView<TrainLine> dataView) {
        ModNetworkManager.GET_ALL_TRAIN_LINES.send(NetworkDirection.toServer(), (response) -> {
            dataView.items.setAll(response.getLines().stream().sorted((a, b) -> a.getLineName().compareToIgnoreCase(b.getLineName())).toList());
        }, () -> {});
    }
    
    private void reloadTrainCategories(OptionsDataView<TrainCategory> dataView) {
        ModNetworkManager.GET_ALL_TRAIN_CATEGORIES.send(NetworkDirection.toServer(), (response) -> {
            dataView.items.setAll(response.getCategories().stream().sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName())).toList());
        }, () -> {});
    }

    private void createStationTagOption() {
        OptionEntryHeader header = new OptionEntryHeader(optionTagTitle, List.of(optionTagDescription));
        header.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new StationTagSettingsWindow(mgr));
            return false;
        });
        optionsView.addEntry(header);
    }

    @SuppressWarnings("unchecked")
    private OptionEntry<String> createTrainBlacklistOption() {
        OptionEntry<String> trainLinesEntry = optionsView.addEntry(new OptionEntry<>(optionTrainBlacklistTitle, List.of(optionTrainBlacklistDescription), (s, e) -> {
            OptionEntry<String> c = (OptionEntry<String>)s;
            c.expanded.toggle();
            if (c.expanded.get()) {
                reloadBlacklistedTrains(c.dataView);
            } 
            return false;
        }));

        
        TableLayout layout = new TableLayout();
        layout.addColumn("name", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("help", 0, ColumnSizeMode.AUTO);
        layout.addColumn("dropdown", 20, ColumnSizeMode.FIXED);
        trainLinesEntry.getHeader().layout.set(layout);

        trainLinesEntry.dataView.createNewItemBuilder.set(!GlobalSettingsClient.modificationsAllowed() ? null : (view) -> {
            OptionsDataView.CreateEntryItem<String> item = new OptionsDataView.CreateEntryItem<>(view);
            
            CreateTextBox nameBox = new CreateTextBox(0, 0, 0);
            FlatIconButton addBtn = new FlatIconButton(0, 0, ModGuiIcons.ADD.getAsSprite(16, 16));
            final Runnable addAction = () -> {
                if (nameBox.text.get().getPlainText().isBlank()) {
                    return;
                }
                ModNetworkManager.ADD_TRAIN_TO_BLACKLIST.send(NetworkDirection.toServer(), new AddTrainToBlacklistPacketData.Request(nameBox.text.get().getPlainText()), (response) -> {
                    reloadBlacklistedTrains(view);
                }, () -> {});
                nameBox.text.get().clear(); 
            };

            nameBox.acceptAndCancelKeysEnabled.set(true);
            nameBox.autocompleteManager.set(new TrainAutocomplete());
            nameBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_ADD), 200));

            item.subComponents.add(new DataSlotComponent("name", nameBox));
            item.subComponents.add(new DataSlotComponent("action", addBtn));
            return item;
        });
        trainLinesEntry.dataView.searchFilter.set((item, searchTerm) -> {
            return item.toLowerCase().contains(searchTerm.toLowerCase());
        });
        trainLinesEntry.dataView.itemBuilder.set((in) -> {
            OptionsDataView.DLBasicItem<String> item = new OptionsDataView.DLBasicItem<>(trainLinesEntry.dataView, in);
            
            TextOptionLabel nameLabel = new TextOptionLabel();
            nameLabel.text.set(in);

            if (GlobalSettingsClient.modificationsAllowed()) {
                FlatIconButton deleteBtn = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
                deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {                    
                    getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, deleteBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                        ModNetworkManager.REMOVE_TRAIN_FROM_BLACKLIST.send(NetworkDirection.toServer(), new RemoveTrainFromBlacklistPacketData.Request(in), (response) -> {
                            reloadBlacklistedTrains(trainLinesEntry.dataView);
                        }, () -> {});
                    }));
                    return false;
                });
                deleteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_DELETE), 200));

                item.subComponents.add(new DataSlotComponent("action", deleteBtn));
            }
            item.subComponents.add(new DataSlotComponent("name", nameLabel));
            return item;
        });
        
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("name", TextUtils.text("Name"), 100, SizeMode.PERCENTAGE));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("action", TextUtils.text("Action"), 18, SizeMode.FIXED));

        FlatIconButton btnHelp = new FlatIconButton(0, 0, ModGuiIcons.HELP.getAsSprite(16, 16));
        btnHelp.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));
        btnHelp.layoutContraint.set("help");
        btnHelp.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_BLACKLIST);
            return false;
        });
        trainLinesEntry.getHeader().addComponent(btnHelp);

        return trainLinesEntry;
    }


    @SuppressWarnings("unchecked")
    private OptionEntry<String> createStationBlacklistOption() {
        OptionEntry<String> trainLinesEntry = optionsView.addEntry(new OptionEntry<>(optionBlacklistTitle, List.of(optionBlacklistDescription), (s, e) -> {
            OptionEntry<String> c = (OptionEntry<String>)s;
            c.expanded.toggle();
            if (c.expanded.get()) {
                reloadBlacklistedStations(c.dataView);
            } 
            return false;
        }));

        TableLayout layout = new TableLayout();
        layout.addColumn("name", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("help", 0, ColumnSizeMode.AUTO);
        layout.addColumn("dropdown", 20, ColumnSizeMode.FIXED);
        trainLinesEntry.getHeader().layout.set(layout);

        trainLinesEntry.dataView.createNewItemBuilder.set(!GlobalSettingsClient.modificationsAllowed() ? null : (view) -> {
            OptionsDataView.CreateEntryItem<String> item = new OptionsDataView.CreateEntryItem<>(view);
            
            CreateTextBox nameBox = new CreateTextBox(0, 0, 0);
            FlatIconButton addBtn = new FlatIconButton(0, 0, ModGuiIcons.ADD.getAsSprite(16, 16));
            final Runnable addAction = () -> {                
                if (nameBox.text.get().getPlainText().isBlank()) {
                    return;
                }
                ModNetworkManager.ADD_STATION_TO_BLACKLIST.send(NetworkDirection.toServer(), new AddStationToBlacklistPacketData.Request(nameBox.text.get().getPlainText()), (response) -> {
                    reloadBlacklistedStations(view);
                }, () -> {});
                nameBox.text.get().clear();
            };

            nameBox.acceptAndCancelKeysEnabled.set(true);
            nameBox.autocompleteManager.set(new StationsAutocomplete());
            nameBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_ADD), 200));

            item.subComponents.add(new DataSlotComponent("name", nameBox));
            item.subComponents.add(new DataSlotComponent("action", addBtn));
            return item;
        });
        trainLinesEntry.dataView.searchFilter.set((item, searchTerm) -> {
            return item.toLowerCase().contains(searchTerm.toLowerCase());
        });
        trainLinesEntry.dataView.itemBuilder.set((in) -> {
            OptionsDataView.DLBasicItem<String> item = new OptionsDataView.DLBasicItem<>(trainLinesEntry.dataView, in);
            
            TextOptionLabel nameLabel = new TextOptionLabel();
            nameLabel.text.set(in);

            FlatIconButton deleteBtn = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, deleteBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                    ModNetworkManager.REMOVE_STATION_FROM_BLACKLIST.send(NetworkDirection.toServer(), new RemoveStationFromBlacklistPacketData.Request(in), (response) -> {
                        reloadBlacklistedStations(trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            deleteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_DELETE), 200));

            item.subComponents.add(new DataSlotComponent("name", nameLabel));
            item.subComponents.add(new DataSlotComponent("action", deleteBtn));
            return item;
        });
        
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("name", TextUtils.text("Name"), 100, SizeMode.PERCENTAGE));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("action", TextUtils.text("Action"), 18, SizeMode.FIXED));
        
        FlatIconButton btnHelp = new FlatIconButton(0, 0, ModGuiIcons.HELP.getAsSprite(16, 16));
        btnHelp.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));
        btnHelp.layoutContraint.set("help");
        btnHelp.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_STATION_BLACKLIST);
            return false;
        });
        trainLinesEntry.getHeader().addComponent(btnHelp);

        return trainLinesEntry;
    }


    @SuppressWarnings("unchecked")
    private OptionEntry<TrainLine> createTrainLineOption() {
        OptionEntry<TrainLine> trainLinesEntry = optionsView.addEntry(new OptionEntry<>(optionTrainLineTitle, List.of(optionTrainLineDescription), (s, e) -> {
            OptionEntry<TrainLine> c = (OptionEntry<TrainLine>)s;
            c.expanded.toggle();
            if (c.expanded.get()) {
                reloadTrainLines(c.dataView);
            } 
            return false;
        }));
        
        TableLayout layout = new TableLayout();
        layout.addColumn("name", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("help", 0, ColumnSizeMode.AUTO);
        layout.addColumn("dropdown", 20, ColumnSizeMode.FIXED);
        trainLinesEntry.getHeader().layout.set(layout);

        trainLinesEntry.dataView.createNewItemBuilder.set(!GlobalSettingsClient.modificationsAllowed() ? null : (view) -> {
            OptionsDataView.CreateEntryItem<TrainLine> item = new OptionsDataView.CreateEntryItem<>(view);
            
            CreateTextBox nameBox = new CreateTextBox(0, 0, 0);
            FlatIconButton addBtn = new FlatIconButton(0, 0, ModGuiIcons.ADD.getAsSprite(16, 16));
            final Runnable addAction = () -> {                
                if (nameBox.text.get().getPlainText().isBlank()) {
                    return;
                }
                ModNetworkManager.CREATE_TRAIN_LINE.send(NetworkDirection.toServer(), new CreateTrainLinePacketData.Request(nameBox.text.get().getPlainText()), (response) -> {
                    reloadTrainLines(view);
                }, () -> {});
                nameBox.text.get().clear(); 
            };

            nameBox.acceptAndCancelKeysEnabled.set(true);
            nameBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_ADD), 200));

            item.subComponents.add(new DataSlotComponent("name", nameBox));
            item.subComponents.add(new DataSlotComponent("action", addBtn));
            return item;
        });
        trainLinesEntry.dataView.searchFilter.set((item, searchTerm) -> {
            return item.getLineName().toLowerCase().contains(searchTerm.toLowerCase());
        });
        trainLinesEntry.dataView.itemBuilder.set((in) -> {
            boolean allowed = in.getOwner().isAllowed() && GlobalSettingsClient.modificationsAllowed();

            OptionsDataView.DLBasicItem<TrainLine> item = new OptionsDataView.DLBasicItem<>(trainLinesEntry.dataView, in);
            TextOptionLabel nameLabel = new TextOptionLabel();
            nameLabel.addEventListener(DLEditableLabel.TextEditedEvent.class, (s, e) -> {                
                if (nameLabel.text.get().isBlank()) {
                    return false;
                }
                ModNetworkManager.UPDATE_TRAIN_LINE_NAME.send(NetworkDirection.toServer(), new UpdateTrainLineNamePacketData.Request(in.getId(), nameLabel.text.get()), (response) -> {
                    reloadTrainLines(trainLinesEntry.dataView);
                }, () -> {});
                return false;
            });
            nameLabel.text.set(in.getLineName());
            nameLabel.editable.set(allowed);

            FlatIconButton btnPermissions = new FlatIconButton(0, 0, in.getOwner().get().getIcon());
            DLContextMenu permissionsMenu = new DLContextMenu((pX, pY) -> {
                List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
                entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRUSTED_PLAYERS), DLSprite.empty(), true, () -> {
                    getWindowManager().createModal((mgr) -> new FlyoutTrustedPlayersWidget(mgr, btnPermissions, FlyoutPointer.RIGHT, ColorShade.DARK, in.getOwner().getTrusted(), (players) -> {                        
                        GlobalSettingsClient.updateTrainLinePermissions(new UpdateTrainLinePermissionsPacketData.Request(in.getId(), null, null, players), (a) -> {
                            reloadTrainLines(trainLinesEntry.dataView);
                        });
                    }));
                }, null));
                entries.add(DLContextMenu.ItemEntry.SEPARATOR);
                entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRANSFER_OWNERSHIP), DLSprite.empty(), true, () -> {
                    getWindowManager().createModal(mgr -> new TransferOwnershipWindow(mgr, in.getOwner().getOwner().orElse(null), (newOwner) -> {
                        GlobalSettingsClient.updateTrainLinePermissions(new UpdateTrainLinePermissionsPacketData.Request(in.getId(), newOwner, null, null), $ -> {
                            GlobalSettingsClient.getTrainLines((res) -> {
                                reloadTrainLines(trainLinesEntry.dataView);
                            });
                        });
                    }));
                }, null));
                return entries;
            });
            btnPermissions.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                if (!in.getOwner().isAllowed()) {
                    return false;
                }
                
                GlobalSettingsClient.updateTrainLinePermissions(new UpdateTrainLinePermissionsPacketData.Request(in.getId(), null, in.getOwner().get().next(), null), (a) -> {
                    a.ifPresent(x -> {
                        in.getOwner().set(x.getOwner().get());
                        in.getOwner().updateTrusted(x.getOwner().getTrusted());
                        btnPermissions.sprite.set(x.getOwner().get().getIcon());
                        btnPermissions.tooltip.set(new DLTooltip(in.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));
                    });
                });
                return false;
            });
            btnPermissions.addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
                if (!in.getOwner().isAdmin()) {
                    return false;
                }
                permissionsMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
                return false;
            });
            btnPermissions.tooltip.set(new DLTooltip(in.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));

            FlatIconButton colorBtn = new FlatIconButton(0, 0, ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16));
            colorBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                getWindowManager().createModal((mgr) -> new FlyoutColorPickerWidget(mgr, colorBtn, FlyoutPointer.RIGHT, ColorShade.DARK, DLColor.TRANSPARENT, Constants.DEFAULT_TRAIN_TYPE_COLORS, 8, true, true, (col) -> {
                    ModNetworkManager.UPDATE_TRAIN_LINE_COLOR.send(NetworkDirection.toServer(), new UpdateTrainLineColorPacketData(in.getId(), col), (response) -> {
                        reloadTrainLines(trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            colorBtn.tooltip.set(new DLTooltip(List.of(textColor), 200));
            colorBtn.backgroundTint.set(in.getColor());

            FlatIconButton deleteBtn = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, deleteBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                    ModNetworkManager.DELETE_TRAIN_LINE.send(NetworkDirection.toServer(), new DeleteTrainLinePacketData(in.getId()), (response) -> {
                        reloadTrainLines(trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            deleteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_DELETE), 200));

            item.subComponents.add(new DataSlotComponent("name", nameLabel));
            item.subComponents.add(new DataSlotComponent("permissions", btnPermissions));
            if (allowed) {
                item.subComponents.add(new DataSlotComponent("color", colorBtn));
                item.subComponents.add(new DataSlotComponent("action", deleteBtn));
            }
            return item;
        });
        
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("name", TextUtils.text("Name"), 100, SizeMode.PERCENTAGE));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("permissions", TextUtils.text("Permission"), 18, SizeMode.FIXED));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("color", TextUtils.text("Color"), 18, SizeMode.FIXED));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("action", TextUtils.text("Action"), 18, SizeMode.FIXED));
        
        FlatIconButton btnHelp = new FlatIconButton(0, 0, ModGuiIcons.HELP.getAsSprite(16, 16));
        btnHelp.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));
        btnHelp.layoutContraint.set("help");
        btnHelp.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_LINES);
            return false;
        });
        trainLinesEntry.getHeader().addComponent(btnHelp);

        return trainLinesEntry;
    }






    @SuppressWarnings("unchecked")
    private OptionEntry<TrainCategory> createTrainCategoryOption() {
        OptionEntry<TrainCategory> trainLinesEntry = optionsView.addEntry(new OptionEntry<>(optionTrainCategoryTitle, List.of(optionTrainCategoryDescription), (s, e) -> {
            OptionEntry<TrainCategory> c = (OptionEntry<TrainCategory>)s;
            c.expanded.toggle();
            if (c.expanded.get()) {
                reloadTrainCategories(c.dataView);
            } 
            return false;
        }));

        TableLayout layout = new TableLayout();
        layout.addColumn("name", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("help", 0, ColumnSizeMode.AUTO);
        layout.addColumn("dropdown", 20, ColumnSizeMode.FIXED);
        trainLinesEntry.getHeader().layout.set(layout);

        trainLinesEntry.dataView.createNewItemBuilder.set(!GlobalSettingsClient.modificationsAllowed() ? null : (view) -> {
            OptionsDataView.CreateEntryItem<TrainCategory> item = new OptionsDataView.CreateEntryItem<>(view);
            
            CreateTextBox nameBox = new CreateTextBox(0, 0, 0);
            FlatIconButton addBtn = new FlatIconButton(0, 0, ModGuiIcons.ADD.getAsSprite(16, 16));
            final Runnable addAction = () -> {                
                if (nameBox.text.get().getPlainText().isBlank()) {
                    return;
                }
                ModNetworkManager.CREATE_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new CreateTrainCategoryPacketData.Request(nameBox.text.get().getPlainText()), (response) -> {
                    reloadTrainCategories(view);
                }, () -> {});
                nameBox.text.get().clear(); 
            };

            nameBox.acceptAndCancelKeysEnabled.set(true);
            nameBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                addAction.run();
                return false;
            });
            addBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_ADD), 200));

            item.subComponents.add(new DataSlotComponent("name", nameBox));
            item.subComponents.add(new DataSlotComponent("action", addBtn));
            return item;
        });
        trainLinesEntry.dataView.searchFilter.set((item, searchTerm) -> {
            return item.getCategoryName().toLowerCase().contains(searchTerm.toLowerCase());
        });
        trainLinesEntry.dataView.itemBuilder.set((in) -> {
            boolean allowed = in.getOwner().isAllowed() && GlobalSettingsClient.modificationsAllowed();

            OptionsDataView.DLBasicItem<TrainCategory> item = new OptionsDataView.DLBasicItem<>(trainLinesEntry.dataView, in);
            TextOptionLabel nameLabel = new TextOptionLabel();
            nameLabel.addEventListener(DLEditableLabel.TextEditedEvent.class, (s, e) -> {
                if (nameLabel.text.get().isBlank()) {
                    return false;
                }
                ModNetworkManager.UPDATE_TRAIN_CATEGORY_NAME.send(NetworkDirection.toServer(), new UpdateTrainCategoryNamePacketData.Request(in.getId(), nameLabel.text.get()), (response) -> {
                    reloadTrainCategories(trainLinesEntry.dataView);
                }, () -> {});
                return false;
            });
            nameLabel.text.set(in.getCategoryName());
            nameLabel.editable.set(allowed);

            FlatIconButton btnPermissions = new FlatIconButton(0, 0, in.getOwner().get().getIcon());
            DLContextMenu permissionsMenu = new DLContextMenu((pX, pY) -> {
                List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
                entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRUSTED_PLAYERS), DLSprite.empty(), true, () -> {
                    getWindowManager().createModal((mgr) -> new FlyoutTrustedPlayersWidget(mgr, btnPermissions, FlyoutPointer.RIGHT, ColorShade.DARK, in.getOwner().getTrusted(), (players) -> {                        
                        GlobalSettingsClient.updateTrainCategoryPermissions(new TrainCategoryUpdatePermissionsPacketData.Request(in.getId(), null, null, players), (a) -> {
                            reloadTrainCategories(trainLinesEntry.dataView);
                        });
                    }));
                }, null));
                entries.add(DLContextMenu.ItemEntry.SEPARATOR);
                entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRANSFER_OWNERSHIP), DLSprite.empty(), true, () -> {
                    getWindowManager().createModal(mgr -> new TransferOwnershipWindow(mgr, in.getOwner().getOwner().orElse(null), (newOwner) -> {
                        GlobalSettingsClient.updateTrainCategoryPermissions(new TrainCategoryUpdatePermissionsPacketData.Request(in.getId(), newOwner, null, null), $ -> {
                            GlobalSettingsClient.getTrainCategories((res) -> {
                                reloadTrainCategories(trainLinesEntry.dataView);
                            });
                        });
                    }));
                }, null));
                return entries;
            });
            btnPermissions.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                if (!in.getOwner().isAllowed()) {
                    return false;
                }
                
                GlobalSettingsClient.updateTrainCategoryPermissions(new TrainCategoryUpdatePermissionsPacketData.Request(in.getId(), null, in.getOwner().get().next(), null), (a) -> {
                    a.ifPresent(x -> {
                        in.getOwner().set(x.getOwner().get());
                        in.getOwner().updateTrusted(x.getOwner().getTrusted());
                        btnPermissions.sprite.set(x.getOwner().get().getIcon());
                        btnPermissions.tooltip.set(new DLTooltip(in.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));
                    });
                });
                return false;
            });
            btnPermissions.addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
                if (!in.getOwner().isAdmin()) {
                    return false;
                }
                permissionsMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
                return false;
            });
            btnPermissions.tooltip.set(new DLTooltip(in.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));

            FlatIconButton colorBtn = new FlatIconButton(0, 0, ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16));
            colorBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                getWindowManager().createModal((mgr) -> new FlyoutColorPickerWidget(mgr, colorBtn, FlyoutPointer.RIGHT, ColorShade.DARK, DLColor.TRANSPARENT, Constants.DEFAULT_TRAIN_TYPE_COLORS, 8, true, true, (col) -> {
                    ModNetworkManager.UPDATE_TRAIN_CATEGORY_COLOR.send(NetworkDirection.toServer(), new UpdateTrainCategoryColorPacketData(in.getId(), col), (response) -> {
                        reloadTrainCategories(trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            colorBtn.tooltip.set(new DLTooltip(List.of(textColor), 200));
            colorBtn.backgroundTint.set(in.getColor());

            FlatIconButton deleteBtn = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, deleteBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                    ModNetworkManager.DELETE_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new DeleteTrainCategoryPacketData(in.getId()), (response) -> {
                        reloadTrainCategories(trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            deleteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_DELETE), 200));

            item.subComponents.add(new DataSlotComponent("name", nameLabel));
            
            item.subComponents.add(new DataSlotComponent("permissions", btnPermissions));
            if (allowed) {
                item.subComponents.add(new DataSlotComponent("color", colorBtn));
                item.subComponents.add(new DataSlotComponent("action", deleteBtn));
            }
            return item;
        });
        
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("name", TextUtils.text("Name"), 100, SizeMode.PERCENTAGE));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("permissions", TextUtils.text("Permission"), 18, SizeMode.FIXED));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("color", TextUtils.text("Color"), 18, SizeMode.FIXED));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("action", TextUtils.text("Action"), 18, SizeMode.FIXED));

        FlatIconButton btnHelp = new FlatIconButton(0, 0, ModGuiIcons.HELP.getAsSprite(16, 16));
        btnHelp.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));
        btnHelp.layoutContraint.set("help");
        btnHelp.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_CATEGORIES);
            return false;
        });
        trainLinesEntry.getHeader().addComponent(btnHelp);

        return trainLinesEntry;
    }
}
