package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;

import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition.TimeUnit;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateScrollInput;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.modular.ModularWidgetContainer;
import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import de.mrjulsen.crn.data.train.StationDepartureHistory.ETrainFilter;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;

public class TrainSeparationSettingsScreen extends DLScreen {

    private static final MutableComponent title = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition.train_separation.settings");
    private static final int GUI_WIDTH = 212;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;
    private static final int LINES = 2;
    private static final int GUI_HEIGHT = headerSize.size() + footerSize.size() + 22 * LINES + 12;

    private int guiLeft, guiTop;
    private GuiAreaDefinition workingArea;

    private DLCreateIconButton backButton;
    private ModularWidgetContainer commonSettingsContainer;

    private final Screen lastScreen;
    private final CompoundTag nbt;

    //private String stationFilter;
    private int minutes = 0;
    private int seconds = 5;
    private int ticks = 0;
    private ETrainFilter filter = ETrainFilter.ANY;
    
    public TrainSeparationSettingsScreen(Screen lastScreen, CompoundTag nbt) {
        super(title);
        this.lastScreen = lastScreen;
        this.nbt = nbt;
        @SuppressWarnings("deprecation")
        int t = nbt.contains(TrainSeparationCondition.NBT_TICKS) ? nbt.getInt(TrainSeparationCondition.NBT_TICKS) : nbt.getInt(TrainSeparationCondition.NBT_TIME) * TimeUnit.values()[nbt.getInt(TrainSeparationCondition.NBT_TIME_UNIT)].ticksPer;
        ticks = t;
        minutes = ticks / 1200;
        ticks %= 1200;
        seconds = ticks / 20;
        ticks %= 20;

        this.filter = ETrainFilter.getByIndex(nbt.getByte(TrainSeparationCondition.NBT_TRAIN_FILTER));
    }

    @Override
    public void onClose() {
        super.onClose();
        nbt.putInt(TrainSeparationCondition.NBT_TICKS, minutes * TimeUnit.MINUTES.ticksPer + seconds * TimeUnit.SECONDS.ticksPer + ticks);
        nbt.putByte(TrainSeparationCondition.NBT_TRAIN_FILTER, filter.getIndex());
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;
        workingArea = new GuiAreaDefinition(guiLeft + 1, guiTop + headerSize.size(), GUI_WIDTH - 2, GUI_HEIGHT - headerSize.size() - footerSize.size());

        // Content
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, 0, 0, 0, GuiAreaDefinition.empty());
        commonSettingsContainer = addRenderableWidget(new ModularWidgetContainer(this, workingArea.getX() + 2, workingArea.getY() + 1, workingArea.getWidth() - 4, 22 * LINES + 10, (w, builder) -> {
            /*
            builder.addLine("text", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TEXT.getAsSprite(16, 16)));
                line.add(new DLCreateTextBox(font, line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, TextUtils.empty()))
                    .setRenderArrow(true)
                    .setResponder((txt) -> {
                        this.stationFilter = txt;
                    });
            });
            */

            /*
            builder.addLine("time_src", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));
                line.add(new DLCreateSelectionScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, 18)
                    .setRenderArrow(true)
                    .forOptions(Arrays.stream(ETimeSource.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                    .titled(TextUtils.translate(timeSource.getEnumTranslationKey(CreateRailwaysNavigator.MOD_ID)))
                    .setState(timeSource.getIndex())
                    .calling((i) -> {
                        timeSource = ETimeSource.getByIndex(i);
                    })
                );
            });
            */
            
            builder.addLine("times", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));
                line.add(new DLCreateScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, 30, 18)
                    .setRenderArrow(true)
                    .titled(Lang.translateDirect("generic.unit.minutes"))
                    .withShiftStep(10)
                    .withRange(0, 901)
                    .setState(minutes)
                    .calling(x -> {
                        minutes = x;
                    })
                );
                line.add(new DLCreateScrollInput(this, line.getCurrentX() + 2, line.getY() + 2, 30, 18)
                    .titled(Lang.translateDirect("generic.unit.seconds"))
                    .withShiftStep(10)
                    .withRange(0, 60)
                    .setState(seconds)
                    .calling(x -> {
                        seconds = x;
                    })
                );
                line.add(new DLCreateScrollInput(this, line.getCurrentX() + 2, line.getY() + 2, 30, 18)
                    .titled(Lang.translateDirect("generic.unit.ticks"))
                    .withShiftStep(5)
                    .withRange(0, 20)
                    .setState(ticks)
                    .calling(x -> {
                        ticks = x;
                    })
                );
            });
            
            /*
            builder.addLine("time", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TIME.getAsSprite(16, 16)));
                line.add(new DLCreateScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, 30, 18)
                    .setRenderArrow(true)
                    .titled(Lang.translateDirect("generic.duration"))
                    .withShiftStep(15)
                    .withRange(0, 121)
                    .setState(ticks)
                    .calling(x -> {
                        ticks = x;
                    })
                );

                line.add(new DLCreateSelectionScrollInput(this, line.getCurrentX() + 4, line.getY() + 2, 65, 18)
                    .forOptions(TimeUnit.translatedOptions())
                    .setState(timeUnit.ordinal())
                    .titled(Lang.translateDirect("generic.timeUnit"))
                    .setState(timeUnit.ordinal())
                    .calling((i) -> {
                        timeUnit = TimeUnit.values()[i];
                    })
                );
            });
            */
            
            builder.addLine("train_filter", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.TRAIN.getAsSprite(16, 16)));
                line.add(new DLCreateSelectionScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, 18)
                    .setRenderArrow(true)
                    .forOptions(Arrays.stream(ETrainFilter.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                    .setState(filter.getIndex())
                    .titled(TextUtils.translate(ETrainFilter.ANY.getEnumTranslationKey(CreateRailwaysNavigator.MOD_ID)))
                    .addHint(TextUtils.translate(ETrainFilter.ANY.getEnumDescriptionTranslationKey(CreateRailwaysNavigator.MOD_ID)))
                    .setState(filter.getIndex())
                    .calling((i) -> {
                        filter = ETrainFilter.getByIndex(i);
                    })
                );
            });
        }, scrollBar, 18, 18, 4, 4));        
        addRenderableWidget(scrollBar);
        
        // Buttons
        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 7 - DEFAULT_ICON_BUTTON_WIDTH, guiTop + GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 17 - DEFAULT_ICON_BUTTON_WIDTH * 2, guiTop + GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_SEPARATION);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderScreenBackground(graphics);
        CreateDynamicWidgets.renderWindow(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), false);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, guiLeft + GUI_WIDTH - 31, guiTop + GUI_HEIGHT - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);

        int commonHeight = commonSettingsContainer.getHeight() + 4;
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX(), workingArea.getY() - 1, workingArea.getWidth(), commonHeight, ContainerColor.PURPLE);
        GuiUtils.drawString(graphics, font, guiLeft + 6, guiTop + 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
    }
}