package de.mrjulsen.crn.block.display.properties.components;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.DepartureBoardDisplayTableSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.components.ITextWidthSetting.TextScaleBounds;
import de.mrjulsen.crn.block.display.properties.components.ITrainTextSetting.ETrainTextComponents;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class GuiBuilderWrapper {

    static void buildColorGui(IColorSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IColorSetting.GUI_LINE_COLORS_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16)));
            line.add(new ColorSlotWidget(
                context.container().getParentScreen(),
                line.getCurrentX() + 4,
                line.y() + 2,
                setting.getFontColor() == 0 ? 0 : (0xFF << 24) | (setting.getFontColor() & 0x00FFFFFF),
                ModUtils.getDyeColors(),
                false,
                false,
                List.of(IColorSetting.textFontColor, IColorSetting.textClickToEdit),
                () -> 0,
                (b) -> setting.setFontColor(b.getSelectedColor())
            ));
            line.add(new ColorSlotWidget(
                context.container().getParentScreen(),
                line.getCurrentX() + 4,
                line.y() + 2,
                setting.getBackColor() == 0 ? 0 : (0xFF << 24) | (setting.getBackColor() & 0x00FFFFFF),
                ModUtils.getDyeColors(),
                false,
                true,
                List.of(IColorSetting.textBackColor, IColorSetting.textClickToEdit),
                () -> 0,
                (b) -> setting.setBackColor(b.getSelectedColor())
            ));
        });
        */
    }

    static void buildCarriageIndexGui(ICarriageIndexSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ICarriageIndexSetting.GUI_LINE_CARRIAGE_INDEX_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.CARRIAGE_NUMBER.getAsSprite(16, 16)));
            int w = 22;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 6, line.y() + 2, w, 18))
                .setRenderArrow(true)
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.carriage_index"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.carriage_index.description"))
                .withRange(0, 100)
                .withShiftStep(4)
                .setState(setting.getCarriageIndex())
                .calling((i) -> {
                    setting.setCarriageIndex(i.byteValue());
                })
            ;
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), ICarriageIndexSetting.textOverwriteCarriageIndex.getString(), setting.shouldOverwriteCarriageIndex(), (c) -> setting.setOverwriteCarriageIndex(c.isChecked())) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(ICarriageIndexSetting.textOverwriteCarriageIndexDescription), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
        });
        */
    }

    static void buildBasicTextWidthGui(ICustomTextWidthSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ICustomTextWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.WIDTH.getAsSprite(16, 16)));
        });
        */
    }

    static void buildPlatformWidthGui(IPlatformWidthSetting setting, GuiBuilderContext context, boolean allowAuto) {
        /*
        context.builder().addToLine(IPlatformWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - IPlatformWidthSetting.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width.description"))
                .withRange(allowAuto ? -1 : 0, 65)
                .withShiftStep(4)
                .setState(setting.getPlatformWidth())
                .format((val) -> {
                    if (val >= 0) {
                        return TextUtils.text(String.valueOf(val) + "px");
                    }
                    return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                })
                .calling((i) -> {
                    setting.setPlatformWidth(i.byteValue());
                })
            ;
        });
        */
    }

    static void buildShowArrivalGui(IShowArrivalSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowArrivalSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TARGET.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowArrivalSetting.textShowArrival.getString(), setting.showArrival(), (cb) -> setting.setShowArrival(cb.isChecked())) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(IShowArrivalSetting.textShowArrivalDescription), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
        });
        */
    }

    static void buildShowDoNotBoardTextGui(IShowDoNotBoardText setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowDoNotBoardText.GUI_LINE_SHOW_DO_NOT_BOARD_TEXT_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.WALK.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowDoNotBoardText.textShowDoNotBoardText.getString(), setting.showDoNotBoardText(), (cb) -> setting.setShowDoNotBoardText(cb.isChecked())) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(IShowDoNotBoardText.textShowDoNotBoardTextDescription), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
        });
        */
    }

    static void buildShowExitGui(IShowExitDirectionSetting settings, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowExitDirectionSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.EXIT.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowExitDirectionSetting.textShowExit.getString(), settings.showExit(), (cb) -> settings.setShowExit(cb.isChecked())));
        });
        */
    }

    static void buildShowLineColorGui(IShowLineColorSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowLineColorSetting.GUI_LINE_SHOW_LINE_COLOR_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowLineColorSetting.textShowLineColor.getString(), setting.showLineColor(), (cb) -> setting.setShowLineColor(cb.isChecked())) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(IShowLineColorSetting.textShowLineColorDescription), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
        });
        */
    }

    static void buildShowConnectionGui(IShowNextConnections setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowNextConnections.GUI_LINE_SHOW_CONNECTIONS_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.CONNECTIONS.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowNextConnections.textShowConnections.getString(), setting.showConnections(), (cb) -> setting.setShowConnection(cb.isChecked())));
        });
        */
    }

    static void buildShowTimeAndDateGui(IShowTimeAndDateSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowTimeAndDateSetting.GUI_LINE_SHOW_TIME_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowTimeAndDateSetting.textShowStats.getString(), setting.showTimeAndDate(), (cb) -> setting.setShowTimeAndDate(cb.isChecked())));
        });
        */
    }

    static void buildShowStatsGui(IShowTrainStatsSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IShowTrainStatsSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TRAIN_INFO.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowTrainStatsSetting.textShowStats.getString(), setting.showStats(), (cb) -> setting.setShowStats(cb.isChecked())));
        });
        */
    }

    static void buildTimeDisplayGui(ITimeDisplaySetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ITimeDisplaySetting.GUI_LINE_TIME_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));            
            line.add(new DLCreateSelectionScrollInput(context.container().getParentScreen(), line.getCurrentX() + 6, line.y() + 2, 32, 18))
                .setRenderArrow(true)
                .forOptions(Arrays.stream(ETimeDisplay.values()).map(x -> TextUtils.translate(x.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                .titled(TextUtils.translate("enum.createrailwaysnavigator.time_display"))
                .addHint(TextUtils.translate("enum.createrailwaysnavigator.time_display.description"))
                .format((val) -> {
                    return TextUtils.translate(ETimeDisplay.getById(val).getValueTranslationKey(CreateRailwaysNavigator.MOD_ID));
                })
                .setState(setting.getTimeDisplay().getId())
                .calling((i) -> {
                    setting.setTimeDisplay(ETimeDisplay.getById(i));
                })
            ;
        });
        */
    }

    static void buildTrainNameGui(ITrainNameWidthSetting setting, GuiBuilderContext context, boolean allowAuto, boolean allowMax) {
        /*
        context.builder().addToLine(ITrainNameWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - ITrainNameWidthSetting.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width.description"))
                .withRange(ITrainNameWidthSetting.MIN_VALUE - (allowAuto ? 1 : 0), ITrainNameWidthSetting.MAX_VALUE + (allowMax ? 1 : 0))
                .withShiftStep(5)
                .setState(setting.getTrainNameWidth())
                .format((val) -> {
                    if (val < 0) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                    } else if (val >= 100) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.max");
                    }
                    return TextUtils.text(String.valueOf(val) + "px");
                })
                .calling((i) -> {
                    setting.setTrainNameWidth(i.byteValue());
                })
            ;
        });
        */
    }

    static void buildTrainTextGui(ITrainTextSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ITrainTextSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT.getAsSprite(16, 16)));
            line.add(new DLCreateSelectionScrollInput(context.container().getParentScreen(), line.getCurrentX() + 6, line.y() + 2, line.getRemainingWidth() - 6, 18))
                .setRenderArrow(true)    
                .forOptions(Arrays.stream(ETrainTextComponents.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                .titled(TextUtils.translate("enum.createrailwaysnavigator.train_text_components"))
                .addHint(TextUtils.translate("enum.createrailwaysnavigator.train_text_components.description"))
                .format((val) -> {
                    return TextUtils.translate(ETrainTextComponents.getById(val).getValueTranslationKey(CreateRailwaysNavigator.MOD_ID));
                })
                .setState(setting.getTrainTextComponents().getId())
                .calling((i) -> {
                    setting.setTrainTextComponents(ETrainTextComponents.getById(i));
                })
            ;
        });
        */
    }

    public static void buildPlatformDisplayFocusGui(PlatformDisplayFocusSettings setting, GuiBuilderContext context) {
        /*
        context.builder().addToLine(PlatformDisplayFocusSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - PlatformDisplayFocusSettings.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width_table"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width.description"))
                .withRange(-1, 100)
                .withShiftStep(5)
                .setState(setting.getTrainNameWidth())
                .format((val) -> {
                    if (val < 0) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                    } else if (val >= 100) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.max");
                    }
                    return TextUtils.text(String.valueOf(val) + "px");
                })
                .calling((i) -> {
                    setting.setTrainNameWidth(i.byteValue());
                })
            ;
        });
        context.builder().addToLine(PlatformDisplayFocusSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - PlatformDisplayFocusSettings.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width_table"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width.description"))
                .withRange(-1, 65)
                .withShiftStep(4)
                .setState(setting.getPlatformWidth())
                .format((val) -> {
                    if (val >= 0) {
                        return TextUtils.text(String.valueOf(val) + "px");
                    }
                    return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                })
                .calling((i) -> {
                    setting.setPlatformWidth(i.byteValue());
                })
            ;
        });


        context.builder().addToLine(PlatformDisplayFocusSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - PlatformDisplayFocusSettings.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width_next"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.train_name_width.description"))
                .withRange(-1, 100)
                .withShiftStep(5)
                .setState(setting.getTrainNameWidthNextStop())
                .format((val) -> {
                    if (val < 0) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                    } else if (val >= 100) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.max");
                    }
                    return TextUtils.text(String.valueOf(val) + "px");
                })
                .calling((i) -> {
                    setting.setTrainNameWidthNextStop(i.byteValue());
                })
            ;
        });
        context.builder().addToLine(PlatformDisplayFocusSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - PlatformDisplayFocusSettings.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width_next"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.platform_width.description"))
                .withRange(-1, 65)
                .withShiftStep(4)
                .setState(setting.getPlatformWidthNextStop())
                .format((val) -> {
                    if (val >= 0) {
                        return TextUtils.text(String.valueOf(val) + "px");
                    }
                    return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
                })
                .calling((i) -> {
                    setting.setPlatformWidthNextStop(i.byteValue());
                })
            ;            
        });
        */
    }

    public static void buildDepartureBoardTableGui(DepartureBoardDisplayTableSettings setting, GuiBuilderContext context) {
        /*
        MutableSingle<ScrollInput> stopovers = new MutableSingle<ScrollInput>(null);
        MutableSingle<ScrollInput> info = new MutableSingle<ScrollInput>(null);
        context.builder().addToLine(DepartureBoardDisplayTableSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - DepartureBoardDisplayTableSettings.USED_LINE_SPACE) / 4 - 3;
            stopovers.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.stopovers_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.stopovers_width.description"))
                .withRange(0, 101)
                .withShiftStep(5)
                .setState((int)(setting.getStopoversWidthPercentage() * 100))
                .format((val) -> {
                    return TextUtils.text(String.valueOf(val) + "%");
                })
                .calling((i) -> {
                    setting.setStopoversWidthPercentageInt(i.byteValue());
                    DLUtils.doIfNotNull(info.getFirst(), x -> x.withRange(0, MathUtils.clamp(101 - i, 0, 101)));
                })
            );
            line.add(stopovers.getFirst());
            if (stopovers.getFirst() != null && info.getFirst() != null) {
                stopovers.getFirst().withRange(0, MathUtils.clamp(101 - info.getFirst().getState(), 0, 101));
                info.getFirst().withRange(0, MathUtils.clamp(101 - stopovers.getFirst().getState(), 0, 101));
            }
        });
        context.builder().addToLine(DepartureBoardDisplayTableSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - DepartureBoardDisplayTableSettings.USED_LINE_SPACE) / 4 - 3;
            info.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_width.description"))
                .withRange(0, 101)
                .withShiftStep(5)
                .setState((int)(setting.getInfoWidthPercentage() * 100))
                .format((val) -> {
                    return TextUtils.text(String.valueOf(val) + "%");
                })
                .calling((i) -> {
                    setting.setInfoWidthPercentageInt(i.byteValue());
                    DLUtils.doIfNotNull(stopovers.getFirst(), x -> x.withRange(0, MathUtils.clamp(101 - i, 0, 101)));
                })
            );
            line.add(info.getFirst());
            if (stopovers.getFirst() != null && info.getFirst() != null) {
                stopovers.getFirst().withRange(0, MathUtils.clamp(101 - info.getFirst().getState(), 0, 101));
                info.getFirst().withRange(0, MathUtils.clamp(101 - stopovers.getFirst().getState(), 0, 101));
            }
        });
        */
    }

    public static void buildStaticTextBaseGui(StaticTextDisplaySettings setting, GuiBuilderContext context) {   
        /*     
        context.builder().addToLine(IColorSetting.GUI_LINE_COLORS_NAME, (line) -> {          
            line.addDLW(new DBNavigatorWidget(line.width() - DBNavigatorWidget.WIDTH, line.y() + line.height() / 2 - 8, 18, setting.getSelectedComponentIndex(), StaticTextDisplaySettings.MAX_COMPONENTS, () -> {
                if (setting.getSelectedComponentIndex() >= setting.getComponentsCount() - 1) {
                    setting.verifyComponents();
                    setting.createNewComponent();
                }
                setting.setSelectedComponentIndex(setting.getSelectedComponentIndex() + 1);
                context.builder().clear();
                context.container().clearWidgets();
                setting.buildGui(context);
                context.builder().build();
            }, () -> {
                setting.setSelectedComponentIndex(setting.getSelectedComponentIndex() - 1);
                setting.verifyComponents();
                context.builder().clear();
                context.container().clearWidgets();
                setting.buildGui(context);
                context.builder().build();
            }));
        });
        */
    }

    static void buildStaticTextGui(IStaticTextSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(IStaticTextSetting.GUI_STATIC_TEXT_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT.getAsSprite(16, 16)));            
            DLCreateTextBox editBox = line.add(new DLCreateTextBox(Minecraft.getInstance().font, line.getCurrentX() + 4, line.y() + 2, line.getRemainingWidth() - 4, TextUtils.empty()) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(
                        TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.static_text"),
                        TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.static_text.description").withStyle(ChatFormatting.GRAY)
                    ), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
            editBox.setMaxLength(2048);
            editBox.setValue(setting.getStaticText());
            editBox.setResponder((val) -> {
                setting.setStaticText(val);
            });
        });
        */
    }

    static void buildTextScaleGui(ITextScaleSetting setting, GuiBuilderContext context) {
        /*
        MutableSingle<ScrollInput> scaleInput = new MutableSingle<ScrollInput>(null);
        MutableSingle<ScrollInput> minScaleInput = new MutableSingle<ScrollInput>(null);
        
        context.builder().addLine(ITextScaleSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.SCALE.getAsSprite(16, 16)));
            int w = (line.getWidth() - ITextScaleSetting.USED_LINE_SPACE) / 4 - 3;
            minScaleInput.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_min_x_scale"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_min_x_scale.description"))
                .withRange(10, 101)
                .withShiftStep(5)
                .setState((int)(setting.getMinXScale() * 100))
                .format((val) -> {
                    return TextUtils.text(String.valueOf(val) + "%");
                })
                .calling((i) -> {
                    setting.setMinXScale((float)i.byteValue() / 100f);
                    DLUtils.doIfNotNull(scaleInput.getFirst(), x -> x.withRange(MathUtils.clamp(i, 10, 101), 101));
                })
            );
            line.add(minScaleInput.getFirst());
            if (scaleInput.getFirst() != null && minScaleInput.getFirst() != null) {
                scaleInput.getFirst().withRange(MathUtils.clamp(minScaleInput.getFirst().getState(), 10, 101), 101);
                minScaleInput.getFirst().withRange(10, MathUtils.clamp(scaleInput.getFirst().getState() + 1, 10, 101));
            }
            w = (line.getWidth() - ITextScaleSetting.USED_LINE_SPACE) / 4 - 3;
            scaleInput.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_x_scale"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_x_scale.description"))
                .withRange(10, 101)
                .withShiftStep(5)
                .setState((int)(setting.getXScale() * 100))
                .format((val) -> {
                    return TextUtils.text(String.valueOf(val) + "%");
                })
                .calling((i) -> {
                    setting.setXScale((float)i.byteValue() / 100f);
                    DLUtils.doIfNotNull(minScaleInput.getFirst(), x -> x.withRange(10, MathUtils.clamp(i + 1, 10, 101)));
                })
            );
            line.add(scaleInput.getFirst());
            if (scaleInput.getFirst() != null && minScaleInput.getFirst() != null) {
                scaleInput.getFirst().withRange(MathUtils.clamp(minScaleInput.getFirst().getState(), 10, 101), 101);
                minScaleInput.getFirst().withRange(10, MathUtils.clamp(scaleInput.getFirst().getState() + 1, 10, 101));
            }

            w = (line.getWidth() - ITextScaleSetting.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_y_scale"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_y_scale.description"))
                .withRange(10, 101)
                .withShiftStep(5)
                .setState((int)(setting.getYScale() * 100))
                .format((val) -> {
                    return TextUtils.text(String.valueOf(val) + "%");
                })
                .calling((i) -> {
                    setting.setYScale((float)i.byteValue() / 100f);
                })
            );
        });
        */
    }

    static void buildTextPosGui(ITextPosSetting setting, GuiBuilderContext context) {
        /*
        MutableSingle<ScrollInput> posYInput = new MutableSingle<ScrollInput>(null);
        MutableSingle<ScrollInput> posXInput = new MutableSingle<ScrollInput>(null);
        MutableSingle<DLCreateIconButton> leftAlignBtn = new MutableSingle<DLCreateIconButton>(null);
        MutableSingle<DLCreateIconButton> centerAlignBtn = new MutableSingle<DLCreateIconButton>(null);
        MutableSingle<DLCreateIconButton> rightAlignBtn = new MutableSingle<DLCreateIconButton>(null);
        
        context.builder().addLine(ITextPosSetting.GUI_LINE_TEXT_POS_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT_LEFT_ALIGNED.getAsSprite(16, 16)));
            int w = (line.getRemainingWidth() - 12 - DLIconButton.DEFAULT_BUTTON_WIDTH * 3) / 2;
            posXInput.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_x"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_x.description"))
                .withRange(0, (int)(ITextPosSetting.MAX_X * 2 + 1))
                .withShiftStep(5)
                .setState((int)(setting.getX() * 2))
                .format((val) -> {
                    return TextUtils.text(String.valueOf((float)val / 2f) + "px");
                })
                .calling((i) -> {
                    setting.setX((float)i.intValue() / 2f);
                })
            );
            line.add(posXInput.getFirst());
            posYInput.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_y"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_y.description"))
                .withRange(0, (int)(ITextPosSetting.MAX_Y * 2 + 1))
                .withShiftStep(5)
                .setState((int)(setting.getY() * 2))
                .format((val) -> {
                    return TextUtils.text(String.valueOf((float)val / 2f) + "px");
                })
                .calling((i) -> {
                    setting.setY((float)i.intValue() / 2f);
                })
            );
            line.add(posYInput.getFirst());
            leftAlignBtn.setFirst(new DLCreateIconButton(line.getCurrentX() + 4, line.y() + 2, ModGuiIcons.TEXT_LEFT_ALIGNED.getAsCreateIcon()) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_left_aligned")), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
            line.add(leftAlignBtn.getFirst());
            centerAlignBtn.setFirst(new DLCreateIconButton(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT_CENTERED.getAsCreateIcon()) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_centered")), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
            line.add(centerAlignBtn.getFirst());
            rightAlignBtn.setFirst(new DLCreateIconButton(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT_RIGH_ALIGNED.getAsCreateIcon()) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_right_aligned")), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
            line.add(rightAlignBtn.getFirst());

            leftAlignBtn.getFirst().withCallback(() -> {
                leftAlignBtn.getFirst().set_active(false);
                centerAlignBtn.getFirst().set_active(true);
                rightAlignBtn.getFirst().set_active(true);
                setting.setTextAlignment(ETextAlignment.LEFT);
            });
            centerAlignBtn.getFirst().withCallback(() -> {
                leftAlignBtn.getFirst().set_active(true);
                centerAlignBtn.getFirst().set_active(false);
                rightAlignBtn.getFirst().set_active(true);
                setting.setTextAlignment(ETextAlignment.CENTER);
            });
            rightAlignBtn.getFirst().withCallback(() -> {
                leftAlignBtn.getFirst().set_active(true);
                centerAlignBtn.getFirst().set_active(true);
                rightAlignBtn.getFirst().set_active(false);
                setting.setTextAlignment(ETextAlignment.RIGHT);
            });
            
            leftAlignBtn.getFirst().set_active(setting.getTextAlignment() != ETextAlignment.LEFT);
            centerAlignBtn.getFirst().set_active(setting.getTextAlignment() != ETextAlignment.CENTER);
            rightAlignBtn.getFirst().set_active(setting.getTextAlignment() != ETextAlignment.RIGHT);
        });
        */
    }

    static void buildTextMaxWidthGui(ITextWidthSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ITextWidthSetting.GUI_LINE_TEXT_MAX_WIDTH_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.WIDTH.getAsSprite(16, 16)));
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, 48, 18))
                .titled(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_max_width"))
                .addHint(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_max_width.description"))
                .withRange((int)ITextWidthSetting.MIN_VALUE * 2, (int)ITextWidthSetting.MAX_VALUE * 2 + 1)
                .withShiftStep(20)
                .setState((int)(setting.getTextMaxWidth() * 2))
                .format((val) -> {
                    if (val >= ITextWidthSetting.MAX_VALUE * 2) {
                        return TextUtils.translate("gui.createrailwaysnavigator.common.max");
                    }
                    return TextUtils.text(String.valueOf((float)val.intValue() / 2f) + "px");
                })
                .calling((i) -> {
                    setting.setTextMaxWidth((float)i.intValue() / 2f);
                })
            ;
            line.add(new DLCreateSelectionScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, line.getRemainingWidth() - 4, 18))
                .forOptions(Arrays.stream(TextScaleBounds.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                .titled(TextUtils.translate(TextScaleBounds.CUT_OFF.getEnumTranslationKey(CreateRailwaysNavigator.MOD_ID)))
                .addHint(TextUtils.translate(TextScaleBounds.CUT_OFF.getEnumDescriptionTranslationKey(CreateRailwaysNavigator.MOD_ID)))
                .format((val) -> {
                    return TextUtils.translate(TextScaleBounds.getByIndex(val).getValueTranslationKey(CreateRailwaysNavigator.MOD_ID));
                })
                .setState(setting.getBoundsAction().getIndex())
                .calling((i) -> {
                    setting.setBoundsAction(TextScaleBounds.getByIndex(i));
                })
            ;
        });
        */
    }

    static void buildTextBackgroundColorGui(ITextBackgroundColorSetting setting, GuiBuilderContext context) {
        /*
        context.builder().addLine(ITextBackgroundColorSetting.GUI_BG_COLOR_NAME, (line) -> {
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16)));
            line.add(new ColorSlotWidget(
                context.container().getParentScreen(),
                line.getCurrentX() + 4,
                line.y() + 2,
                setting.getTextBackgroundColor() == 0 ? 0 : (0xFF << 24) | (setting.getTextBackgroundColor() & 0x00FFFFFF),
                Constants.DEFAULT_TRAIN_TYPE_COLORS,
                true,
                true,
                List.of(ITextBackgroundColorSetting.txtLabelBackgroundColor, IColorSetting.textClickToEdit),
                () -> (int)context.container().getYScrollOffset(),
                (b) -> setting.setTextBackgroundColor(b.getSelectedColor())
            ));
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), ITextBackgroundColorSetting.txtFullSize.getString(), setting.isFullLabelBackgroundColor(), (c) -> setting.setFullLabelBackgroundColor(c.isChecked())) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(context.container().getParentScreen(), this, List.of(ITextBackgroundColorSetting.txtFullSizeDescription), context.container().getParentScreen().width() / 3, graphics, mouseX, mouseY);
                }
            });
        });
        */
    }
    
}
