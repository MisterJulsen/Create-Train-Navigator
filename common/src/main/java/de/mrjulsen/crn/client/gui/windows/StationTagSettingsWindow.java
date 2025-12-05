package de.mrjulsen.crn.client.gui.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.FlyoutConfirmDialog;
import de.mrjulsen.crn.client.gui.flyout.FlyoutTrustedPlayersWidget;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.SearchBox;
import de.mrjulsen.crn.client.gui.widgets.SearchBox.SearchBoxRenderer;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.options.NewEntryComponent;
import de.mrjulsen.crn.client.gui.widgets.options.OptionEntry;
import de.mrjulsen.crn.client.gui.widgets.options.OptionsDataView;
import de.mrjulsen.crn.client.gui.widgets.options.OptionsView;
import de.mrjulsen.crn.client.gui.widgets.options.TextOptionLabel;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.crn.network.packets.pain.AddStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagRequestByTagPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagNamePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.Lock;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLEditableLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout.ColumnSizeMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.DataSlot;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.DataSlotComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView.SizeMode;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class StationTagSettingsWindow extends AbstractNavigatorScreen {

    private final MutableComponent tooltipDeleteTag = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.delete_tag");
    private final MutableComponent textStationName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.hint.station_name");
    private final MutableComponent textPlatformName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.hint.platform");

    private final OptionsView optionsView;
    private final SearchBox searchBox;

    public StationTagSettingsWindow(DLWindowManager manager) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.title").append(GlobalSettingsClient.modificationsAllowed() ? TextUtils.empty() : TextUtils.text(" ").append(Constants.TEXT_READ_ONLY).withStyle(ChatFormatting.DARK_RED)), ContainerColor.PURPLE, BarColor.GRAY);
        manager.setPauseScreen(false);
        
        CreateButton helpButton = addComponent(new CreateButton(width() - CreateButton.WIDTH - 8, height() - CreateButton.HEIGHT - 6, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> { 
            Util.getPlatform().openUri(Constants.HELP_PAGE_STATION_TAGS);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));

        this.searchBox = addComponent(new SearchBox(10, FooterSize.DEFAULT.size() + 5, width() - 20));
        this.searchBox.acceptAndCancelKeysEnabled.set(true);
        this.searchBox.componentRenderer.set(new SearchBoxRenderer(true));
        this.searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            reloadTags();
            return false;
        });
        this.searchBox.placeholderText.set(Constants.TEXT_SEARCH);
        optionsView = addComponent(new OptionsView(3, FooterSize.DEFAULT.size() + 19, GUI_WIDTH - 6, GUI_HEIGHT - (FooterSize.DEFAULT.size() + 1) - FooterSize.SMALL.size() - 19));

        reloadTags();
    }
    
    private void reloadTags() {
        optionsView.clearEntries();
        ModNetworkManager.GET_ALL_STATION_TAGS.send(NetworkDirection.toServer(), (response) -> {
            optionsView.addEntry(new NewEntryComponent(0, 0, 100, (txt) -> {
                if (txt.isBlank()) {
                    return;
                }
                ModNetworkManager.CREATE_STATION_TAG.send(NetworkDirection.toServer(), new CreateStationTagPacketData.Request(txt, Optional.of(new Owner(Minecraft.getInstance().player))), (res) -> {
                    reloadTags();
                }, () -> {});
            }));
            for (StationTag tag : response.getTags()) {
                if (!tag.getTagName().get().toLowerCase().contains(searchBox.text.get().getPlainText().toLowerCase())) {
                    continue;
                }
                createStationTagOption(tag);
            }
        }, () -> {});
    }
    
    private void reloadTag(StationTag tag, OptionsDataView<Pair<String, StationInfo>> dataView) {
        ModNetworkManager.GET_STATION_TAG_BY_TAG.send(NetworkDirection.toServer(), new StationTagRequestByTagPacketData.Request(tag.getTagName()), (response) -> {
            dataView.items.setAll(response.getTag().getAllStations().entrySet().stream()
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .map(x -> new Pair<>(x.getKey(), x.getValue()))
                .toList()
            );
        }, () -> {});
    }

    @SuppressWarnings("unchecked")
    private void createStationTagOption(StationTag tag) {
        DLEditableLabel tagRenameBox = new DLEditableLabel(2, 2, 50, 16);
        tagRenameBox.text.set(tag.getTagName().get());
        tagRenameBox.editable.set(tag.getOwner().isAllowed());
        tagRenameBox.visible.set(false);  
        tagRenameBox.layoutContraint.set("name");    

        OptionEntry<Pair<String, StationInfo>> trainLinesEntry = optionsView.addEntry(new OptionEntry<>(TextUtils.text(tag.getTagName().get()), List.of(
            TextUtils.text(tag.getTagName().get()),
            TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".station_tags.summary", TextUtils.text(String.valueOf(tag.getAllStationNames().size())).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY),
            TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.last_edited",
                tag.getLastEditor().map(x -> {
                    return x.name().isBlank()
                        ? TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.unknown").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC)
                        : TextUtils.text(x.name()).withStyle(ChatFormatting.GREEN);
                }).orElse(TextUtils.text("Server").withStyle(ChatFormatting.GREEN)),
                TextUtils.text(tag.getLastEditedTimeFormatted()).withStyle(ChatFormatting.GREEN)
            ).withStyle(ChatFormatting.GRAY)
        ), (s, e) -> {
            OptionEntry<Pair<String, StationInfo>> c = (OptionEntry<Pair<String, StationInfo>>)s;
            c.expanded.toggle();
            if (c.expanded.get()) {
                reloadTag(tag, c.dataView);
            } 
            tagRenameBox.visible.set(c.expanded.get());
            return false;
        }));

        tagRenameBox.addEventListener(DLEditableLabel.TextEditedEvent.class, (s, e) -> {            
            if (!tag.getOwner().isAllowed()) {
                return false;
            }            
            if (tagRenameBox.text.get().isBlank()) {
                return false;
            }
            ModNetworkManager.UPDATE_STATION_TAG_NAME.send(NetworkDirection.toServer(), new UpdateStationTagNamePacketData(tag.getId(), tagRenameBox.text.get()), (response) -> {
                reloadTags();
            }, () -> {});
            return false;
        });

        
        TableLayout layout = new TableLayout();
        layout.addColumn("gap0", 5, ColumnSizeMode.FIXED);
        layout.addColumn("name", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("gap1", 5, ColumnSizeMode.FIXED);
        layout.addColumn("permissions", 0, ColumnSizeMode.AUTO);
        layout.addColumn("delete", 0, ColumnSizeMode.AUTO);
        layout.addColumn("dropdown", 20, ColumnSizeMode.FIXED);
        trainLinesEntry.getHeader().layout.set(layout);

        trainLinesEntry.dataView.createNewItemBuilder.set((!GlobalSettingsClient.modificationsAllowed() || !tag.getOwner().isAllowed()) ? null : (view) -> {
            OptionsDataView.CreateEntryItem<Pair<String, StationInfo>> item = new OptionsDataView.CreateEntryItem<>(view);
                            
            CreateTextBox nameBox = new CreateTextBox(0, 0, 0);
            nameBox.tooltip.set(new DLTooltip(List.of(textStationName), 200));
            nameBox.autocompleteManager.set(new StationsAutocomplete());
            CreateTextBox platformBox = new CreateTextBox(0, 0, 0);
            platformBox.tooltip.set(new DLTooltip(List.of(textPlatformName), 200));
            FlatIconButton addBtn = new FlatIconButton(0, 0, ModGuiIcons.ADD.getAsSprite(16, 16));            
            addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {           
                if (nameBox.text.get().getPlainText().isBlank() || platformBox.text.get().getPlainText().isBlank()) {
                    return false;
                }     
                ModNetworkManager.ADD_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new AddStationTagEntryPacketData.Request(tag.getId(), nameBox.text.get().getPlainText(), new StationInfo(platformBox.text.get().getPlainText())), (response) -> {
                    reloadTag(tag, view);
                }, () -> {});
                nameBox.text.get().clear(); 
                return false;
            });
            addBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_ADD), 200));

            item.subComponents.add(new DataSlotComponent("name", nameBox));
            item.subComponents.add(new DataSlotComponent("platform", platformBox));
            item.subComponents.add(new DataSlotComponent("action", addBtn));
            return item;
        });
        trainLinesEntry.dataView.searchFilter.set((item, searchTerm) -> {
            return item.getFirst().toLowerCase().contains(searchTerm.toLowerCase());
        });
        trainLinesEntry.dataView.itemBuilder.set((in) -> {
            boolean allowed = tag.getOwner().isAllowed() && GlobalSettingsClient.modificationsAllowed();
            OptionsDataView.DLBasicItem<Pair<String, StationInfo>> item = new OptionsDataView.DLBasicItem<>(trainLinesEntry.dataView, in);
            
            TextOptionLabel nameLabel = new TextOptionLabel();
            nameLabel.text.set(in.getFirst());

            TextOptionLabel platformLbl = new TextOptionLabel();
            platformLbl.addEventListener(DLEditableLabel.TextEditedEvent.class, (s, e) -> {                
                if (platformLbl.text.get().isBlank()) {
                    return false;
                }
                ModNetworkManager.UPDATE_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new UpdateStationTagEntryPacketData.Request(tag.getId(), in.getFirst(),new StationInfo( platformLbl.text.get())), (response) -> {
                    reloadTag(tag, trainLinesEntry.dataView);
                }, () -> {});
                return false;
            });
            platformLbl.text.set(in.getSecond().platform());
            platformLbl.editable.set(allowed);

            FlatIconButton deleteBtn = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {                
                getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, s, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                    ModNetworkManager.REMOVE_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new RemoveStationTagEntryPacketData.Request(tag.getId(), in.getFirst()), (response) -> {
                        reloadTag(tag, trainLinesEntry.dataView);
                    }, () -> {});
                }));
                return false;
            });
            deleteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_DELETE), 200));

            item.subComponents.add(new DataSlotComponent("name", nameLabel));
            item.subComponents.add(new DataSlotComponent("platform", platformLbl));
            if (allowed) {
                item.subComponents.add(new DataSlotComponent("action", deleteBtn));
            }
            return item;
        });
        
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("name", TextUtils.text("Name"), 100, SizeMode.PERCENTAGE));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("platform", TextUtils.text("Platform"), 50, SizeMode.FIXED));
        trainLinesEntry.dataView.dataSlots.add(new DataSlot("action", TextUtils.text("Action"), 18, SizeMode.FIXED));

        
        FlatIconButton btnDelete = new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16));
        btnDelete.tooltip.set(new DLTooltip(List.of(tooltipDeleteTag), 200));
        btnDelete.layoutContraint.set("delete");
        btnDelete.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (!tag.getOwner().isAllowed()) {
                return false;
            }
            getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, s, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                GlobalSettingsClient.deleteStationTag(tag.getId(), () -> {
                    reloadTags();
                });
            }));
            return false;
        });        
        if (tag.getOwner().isAllowed()) {
            trainLinesEntry.getHeader().addComponent(btnDelete);
        }
        
        FlatIconButton btnPermissions = trainLinesEntry.getHeader().addComponent(new FlatIconButton(0, 0, tag.getOwner().get().getIcon()));    
        btnPermissions.layoutContraint.set("permissions"); 
        
        DLContextMenu permissionsMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRUSTED_PLAYERS), DLSprite.empty(), true, () -> {
                getWindowManager().createModal((mgr) -> new FlyoutTrustedPlayersWidget(mgr, btnPermissions, FlyoutPointer.RIGHT, ColorShade.DARK, tag.getOwner().getTrusted(), (players) -> {                        
                    GlobalSettingsClient.updateStationTagPermissions(new StationTagUpdatePermissionsPacketData.Request(tag.getId(), null, null, players), (a) -> {
                        reloadTags();
                    });
                }));
            }, null));
            entries.add(DLContextMenu.ItemEntry.SEPARATOR);
            entries.add(new DLContextMenu.ItemEntry(TextUtils.translate(Lock.TRANSLATION_KEY_TRANSFER_OWNERSHIP), DLSprite.empty(), true, () -> {
                getWindowManager().createModal(mgr -> new TransferOwnershipWindow(mgr, tag.getOwner().getOwner().orElse(null), (newOwner) -> {
                    GlobalSettingsClient.updateStationTagPermissions(new StationTagUpdatePermissionsPacketData.Request(tag.getId(), newOwner, null, null), $ -> {
                        GlobalSettingsClient.getStationTags((res) -> {
                            reloadTags();
                        });
                    });
                }));
            }, null));
            return entries;
        });
        btnPermissions.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (!tag.getOwner().isAllowed()) {
                return false;
            }
            
            GlobalSettingsClient.updateStationTagPermissions(new StationTagUpdatePermissionsPacketData.Request(tag.getId(), null, tag.getOwner().get().next(), null), (a) -> {
                a.ifPresent(x -> {
                    tag.getOwner().set(x.getOwner().get());
                    tag.getOwner().updateTrusted(x.getOwner().getTrusted());
                    btnPermissions.sprite.set(x.getOwner().get().getIcon());
                    btnPermissions.tooltip.set(new DLTooltip(tag.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));
                });
            });
            return false;
        });
        btnPermissions.addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
            if (!tag.getOwner().isAdmin()) {
                return false;
            }
            permissionsMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
            return false;
        });
        btnPermissions.tooltip.set(new DLTooltip(tag.getOwner().asText(new Owner(Minecraft.getInstance().player)), 200));

        trainLinesEntry.getHeader().addComponent(tagRenameBox);
    }






    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
    }    
}
