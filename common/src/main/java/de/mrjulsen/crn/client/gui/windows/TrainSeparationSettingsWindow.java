package de.mrjulsen.crn.client.gui.windows;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition.TimeUnit;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.ModularWidgetContainer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.client.gui.widgets.create.CreateScrollNumberInput;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import de.mrjulsen.crn.data.train.DepartureHistory.ETrainFilter;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCycleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLNumberPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime.TimeSnapshot;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class TrainSeparationSettingsWindow extends DLWindow {

    private static final MutableComponent title = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition.train_separation.settings");

	private static final ItemStack DISPLAY_ITEM = new ItemStack(AllItems.SCHEDULE.get());
    private static final int GUI_WIDTH = 212;
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;

    private ModularWidgetContainer commonSettingsContainer;

    private final CompoundTag nbt;

    private DLTime currentTime = Constants.NULL_TIME;
    private ETrainFilter filter = ETrainFilter.ANY;
    private ETimeSource timeSource = ETimeSource.REAL_LIFE;

    
    public TrainSeparationSettingsWindow(DLWindowManager manager, CompoundTag nbt) {
        super(manager);
        setWidth(GUI_WIDTH);
        windowSpawnPosition.set(WindowPosition.PARENT_CENTER);
        this.nbt = nbt;

        this.currentTime = DLTime.fromTicks(nbt.contains(TrainSeparationCondition.NBT_TICKS) ? nbt.getInt(TrainSeparationCondition.NBT_TICKS) : nbt.getInt(TrainSeparationCondition.NBT_TIME) * TimeUnit.values()[nbt.getInt(TrainSeparationCondition.NBT_TIME_UNIT)].ticksPer, VanillaTimeSystem.INSTANCE);
        this.filter = ETrainFilter.getByIndex(nbt.getByte(TrainSeparationCondition.NBT_TRAIN_FILTER));
        this.timeSource = ETimeSource.getByIndex(nbt.getByte(TrainSeparationCondition.NBT_TIME_SOURCE));


        // Content
        CreateButton backButton = addComponent(new CreateButton(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        
        CreateButton helpButton = addComponent(new CreateButton(width() - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_TRAIN_SEPARATION);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));


        commonSettingsContainer = addComponent(new ModularWidgetContainer(3, headerSize.size() + 1, width() - 6, 100));
        int minHeight = headerSize.size() + footerSize.size() + 2;

        initGui();

        setHeight(minHeight);
        commonSettingsContainer.addEventListener(ModularWidgetContainer.ContentLayoutUpdatedEvent.class, (s, e) -> {
            commonSettingsContainer.setHeight(e.layoutResult().contentHeight());
            setHeight(minHeight + e.layoutResult().contentHeight());
            backButton.setPosition(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT);
            helpButton.setPosition(width() - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT);
            setY(getWindowManager().getScreenHeight() / 2 - height() / 2);
            return false;
        });
    }

    private void initGui() {
        commonSettingsContainer.clearLines();

        DLPanel lineTimes = commonSettingsContainer.addLine("times");
        IconSlotWidget timesIcon = lineTimes.addComponent(new IconSlotWidget(0, 0));
        timesIcon.icon.set(ModGuiIcons.TIME.getAsSprite(16, 16));

        switch (timeSource) {
            case IN_GAME -> {
                TimeSnapshot snapshot = this.currentTime.decomposeGameTime();

                CreateScrollNumberInput daysBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                daysBox.title.set(CreateLang.translateDirect("generic.unit.days"));
                daysBox.shiftStep.set(5D);
                daysBox.min.set(0D);
                daysBox.max.set(49D);
                daysBox.value.set((double)snapshot.days());
                daysBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                    TimeSnapshot ts = this.currentTime.decomposeGameTime();
                    this.currentTime = DLTime.fromIngame((long)e.value(), ts.hours(), ts.minutes(), ts.seconds(), VanillaTimeSystem.INSTANCE);
                    return false;
                });

                CreateScrollNumberInput hoursBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                hoursBox.title.set(CreateLang.translateDirect("generic.unit.hours"));
                hoursBox.shiftStep.set(8D);
                hoursBox.min.set(0D);
                hoursBox.max.set(23D);
                hoursBox.value.set((double)snapshot.hours());
                hoursBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                    TimeSnapshot ts = this.currentTime.decomposeGameTime();
                    this.currentTime = DLTime.fromIngame(ts.days(), (int)e.value(), ts.minutes(), ts.seconds(), VanillaTimeSystem.INSTANCE);
                    return false;
                });

                CreateScrollNumberInput minutesBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                minutesBox.title.set(CreateLang.translateDirect("generic.unit.minutes"));
                minutesBox.shiftStep.set(5D);
                minutesBox.min.set(0D);
                minutesBox.max.set(59D);
                minutesBox.value.set((double)snapshot.minutes());
                minutesBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                    TimeSnapshot ts = this.currentTime.decomposeGameTime();
                    this.currentTime = DLTime.fromIngame(ts.days(), ts.hours(), (int)e.value(), 0, VanillaTimeSystem.INSTANCE);
                    return false;
                });
            }
            default -> {
                TimeSnapshot snapshot = this.currentTime.decomposeRealTime();
                CreateScrollNumberInput minutesBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                minutesBox.title.set(CreateLang.translateDirect("generic.unit.minutes"));
                minutesBox.shiftStep.set(10D);
                minutesBox.min.set(0D);
                minutesBox.max.set(999D);
                minutesBox.value.set((double)(snapshot.minutes() + (snapshot.hours() + snapshot.days() * 24) * 60));
                minutesBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                    TimeSnapshot ts = this.currentTime.decomposeRealTime();
                    this.currentTime = DLTime.fromReal(ts.days(), ts.hours(), (int)e.value(), ts.seconds(), ts.millis(), VanillaTimeSystem.INSTANCE);
                    return false;
                });

                CreateScrollNumberInput secondsBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                secondsBox.title.set(CreateLang.translateDirect("generic.unit.seconds"));
                secondsBox.shiftStep.set(10D);
                secondsBox.min.set(0D);
                secondsBox.max.set(59D);
                secondsBox.value.set((double)snapshot.seconds());
                secondsBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                    TimeSnapshot ts = this.currentTime.decomposeRealTime();
                    this.currentTime = DLTime.fromReal(ts.days(), ts.hours(), ts.minutes(), (int)e.value(), ts.millis(), VanillaTimeSystem.INSTANCE);
                    return false;
                });

                CreateScrollNumberInput ticksBox = lineTimes.addComponent(new CreateScrollNumberInput(0, 0, 22));
                ticksBox.title.set(CreateLang.translateDirect("generic.unit.ticks"));
                ticksBox.shiftStep.set(5D);
                ticksBox.min.set(0D);
                ticksBox.max.set(19D);
                ticksBox.value.set((double)snapshot.millis());
                ticksBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {

                    return false;
                });
            }
        }
        
        CreateItemPicker<ETimeSource> timeSourcePicker = lineTimes.addComponent(new CreateItemPicker<>(0, 0, 80));
        timeSourcePicker.title.set(ETimeSource.IN_GAME.getEnumTranslation());
        timeSourcePicker.hint.set(ETimeSource.IN_GAME.getEnumDescriptionTranslation());
        timeSourcePicker.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        timeSourcePicker.items.addAll(ETimeSource.values());
        timeSourcePicker.selectedItem.set(Optional.ofNullable(timeSource));

        timeSourcePicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            timeSourcePicker.selectedItem.get().ifPresent(i -> timeSource = i);
            initGui();
            return false;
        });



        
        DLPanel lineFilter = commonSettingsContainer.addLine("filter");
        IconSlotWidget filterIcon = lineFilter.addComponent(new IconSlotWidget(0, 0));
        filterIcon.icon.set(ModGuiIcons.TRAIN.getAsSprite(16, 16));

        CreateItemPicker<ETrainFilter> filterType = lineFilter.addComponent(new CreateItemPicker<>(0, 0, 120));
        filterType.renderArrow.set(true);
        filterType.title.set(ETrainFilter.ANY.getEnumTranslation());
        filterType.hint.set(ETrainFilter.ANY.getEnumDescriptionTranslation());
        filterType.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        filterType.items.addAll(ETrainFilter.values());
        filterType.selectedItem.set(Optional.ofNullable(filter));

        filterType.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            filterType.selectedItem.get().ifPresent(i -> filter = i);
            return false;
        });
    }

    @Override
    public void close() {
        nbt.putInt(TrainSeparationCondition.NBT_TICKS, (int)currentTime.getTicks());
        nbt.putByte(TrainSeparationCondition.NBT_TIME_SOURCE, timeSource.getIndex());
        nbt.putByte(TrainSeparationCondition.NBT_TRAIN_FILTER, filter.getIndex());
    }
    
    @Override
    public Rectangle getRenderBounds() {
        return Rectangle.INFINITE;
    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), true);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, width() - 31, height() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
        
        GuiUtils.drawTexture(CRNGui.GUI, graphics, width() - 3, height() - 24, 11, 18, 0, 12);
        GuiGameElement.of(DISPLAY_ITEM).<GuiGameElement
			.GuiRenderBuilder>at(width() + 11, height() - 48, -200)
			.scale(4f)
			.render(graphics.graphics());

    }
}