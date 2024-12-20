package de.mrjulsen.crn.block.display.properties.components;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.DepartureBoardDisplayTableSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.block.display.properties.components.ITrainTextSetting.ETrainTextComponents;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.ColorSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.DLCreateScrollInput;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class GuiBuilderWrapper {

    static void buildColorGui(IColorSetting setting, GuiBuilderContext context) {
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
    }

    static void buildCarriageIndexGui(ICarriageIndexSetting setting, GuiBuilderContext context) {
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
    }

    static void buildBasicTextWidthGui(ICustomTextWidthSetting setting, GuiBuilderContext context) {
        context.builder().addLine(ICustomTextWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.WIDTH.getAsSprite(16, 16)));
        });
    }

    static void buildPlatformWidthGui(IPlatformWidthSetting setting, GuiBuilderContext context, boolean allowAuto) {
        context.builder().addToLine(IPlatformWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - IPlatformWidthSetting.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width.description"))
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
    }

    static void buildShowArrivalGui(IShowArrivalSetting setting, GuiBuilderContext context) {
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
    }

    static void buildShowExitGui(IShowExitDirectionSetting settings, GuiBuilderContext context) {
        context.builder().addLine(IShowExitDirectionSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.EXIT.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowExitDirectionSetting.textShowExit.getString(), settings.showExit(), (cb) -> settings.setShowExit(cb.isChecked())));
        });
    }

    static void buildShowLineColorGui(IShowLineColorSetting setting, GuiBuilderContext context) {
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
    }

    static void buildShowConnectionGui(IShowNextConnections setting, GuiBuilderContext context) {
        context.builder().addLine(IShowNextConnections.GUI_LINE_SHOW_CONNECTIONS_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.CONNECTIONS.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowNextConnections.textShowConnections.getString(), setting.showConnections(), (cb) -> setting.setShowConnection(cb.isChecked())));
        });
    }

    static void buildShowTimeAndDateGui(IShowTimeAndDateSetting setting, GuiBuilderContext context) {
        context.builder().addLine(IShowTimeAndDateSetting.GUI_LINE_SHOW_TIME_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowTimeAndDateSetting.textShowStats.getString(), setting.showTimeAndDate(), (cb) -> setting.setShowTimeAndDate(cb.isChecked())));
        });
    }

    static void buildShowStatsGui(IShowTrainStatsSetting setting, GuiBuilderContext context) {
        context.builder().addLine(IShowTrainStatsSetting.GUI_LINE_SHOW_ARRIVAL_NAME, (line) -> {            
            line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TRAIN_INFO.getAsSprite(16, 16)));            
            line.add(new DLCheckBox(line.getCurrentX() + 4, line.y() + line.height() / 2 - 8, line.getRemainingWidth(), IShowTrainStatsSetting.textShowStats.getString(), setting.showStats(), (cb) -> setting.setShowStats(cb.isChecked())));
        });
    }

    static void buildTimeDisplayGui(ITimeDisplaySetting setting, GuiBuilderContext context) {
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
    }

    static void buildTrainNameGui(ITrainNameWidthSetting setting, GuiBuilderContext context, boolean allowAuto, boolean allowMax) {
        context.builder().addToLine(ITrainNameWidthSetting.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - ITrainNameWidthSetting.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width.description"))
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
    }

    static void buildTrainTextGui(ITrainTextSetting setting, GuiBuilderContext context) {
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
    }

    public static void buildPlatformDisplayFocusGui(PlatformDisplayFocusSettings setting, GuiBuilderContext context) {
        context.builder().addToLine(PlatformDisplayFocusSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - PlatformDisplayFocusSettings.USED_LINE_SPACE) / 4 - 3;
            line.add(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18))
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width_table"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width.description"))
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
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width_table"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width.description"))
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
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width_next"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width.description"))
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
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width_next"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width.description"))
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
    }

    public static void buildDepartureBoardTableGui(DepartureBoardDisplayTableSettings setting, GuiBuilderContext context) {        
        MutableSingle<ScrollInput> stopovers = new MutableSingle<ScrollInput>(null);
        MutableSingle<ScrollInput> info = new MutableSingle<ScrollInput>(null);
        context.builder().addToLine(DepartureBoardDisplayTableSettings.GUI_LINE_TEXT_SIZE_NAME, (line) -> {
            int w = (line.getWidth() - DepartureBoardDisplayTableSettings.USED_LINE_SPACE) / 4 - 3;
            stopovers.setFirst(new DLCreateScrollInput(context.container().getParentScreen(), line.getCurrentX() + 4, line.y() + 2, w, 18)
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.stopovers_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.stopovers_width.description"))
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
                .titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.info_width"))
                .addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.info_width.description"))
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
    }
    
}
