package de.mrjulsen.crn.client;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition.TimeUnit;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.NavigatorToast;
import de.mrjulsen.crn.client.gui.screen.AdvancedDisplaySettingsScreen;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.TrainDebugScreen;
import de.mrjulsen.crn.client.gui.screen.TrainSectionSettingsScreen;
import de.mrjulsen.crn.client.gui.widgets.ResizableButton;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.schedule.instruction.ResetTimingsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import de.mrjulsen.crn.mixin.ModularGuiLineBuilderAccessor;
import de.mrjulsen.crn.mixin.ScheduleScreenAccessor;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ClientWrapper {
    
    private static ELanguage currentLanguage;
    private static Language currentClientLanguage;
    
    public static void showNavigatorGui() {
        DLScreen.setScreen(new NavigatorScreen(null));
    }

    @SuppressWarnings("resource")
    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    public static void handleErrorMessagePacket(ServerErrorPacket packet, Supplier<PacketContext> ctx) {        
        Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, Constants.TEXT_SERVER_ERROR, TextUtils.text(packet.message)));   
    }
    
    public static void showAdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        DLScreen.setScreen(new AdvancedDisplaySettingsScreen(blockEntity));
    }

    public static void updateLanguage(ELanguage lang, boolean force) {
        if (currentLanguage == lang && !force) {
            return;
        }

        LanguageInfo info = lang == ELanguage.DEFAULT ? null : Minecraft.getInstance().getLanguageManager().getLanguage(lang.getCode());
        if (info == null) {
            info = Minecraft.getInstance().getLanguageManager().getLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
        }
        currentLanguage = lang;
        if (lang == ELanguage.DEFAULT || info == null) {
            currentClientLanguage = Language.getInstance();
            CreateRailwaysNavigator.LOGGER.info("Updated custom language to: (Default)");
        } else {
            currentClientLanguage = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(lang == ELanguage.DEFAULT ? Minecraft.getInstance().getLanguageManager().getSelected() : lang.getCode()), false);
            CreateRailwaysNavigator.LOGGER.info("Updated custom language to: " + (info == null ? null : info.name()));
        }
    }

    public static Language getCurrentClientLanguage() {
        return currentClientLanguage == null ? Language.getInstance() : currentClientLanguage;
    }

    
    public static void sendCRNNotification(Component title, Component description) {
        if (ModClientConfig.ROUTE_NOTIFICATIONS.get()) {
            Minecraft.getInstance().getToasts().addToast(NavigatorToast.multiline(title, description));
        }
    }

    public static int renderMultilineLabelSafe(Graphics graphics, int x, int y, Font font, Component text, int maxWidth, int color) {
        MultiLineLabel label = MultiLineLabel.create(font, text, maxWidth);
        label.renderLeftAlignedNoShadow(graphics.graphics(), x, y, font.lineHeight, color);
        return font.lineHeight * label.getLineCount();
    }

    public static int getTextBlockHeight(Font font, Component text, int maxWidth) {
        int lines = font.split(text, maxWidth).size();
        return lines * font.lineHeight;
    }

    public static void showTrainDebugScreen() {
        RenderSystem.recordRenderCall(() -> {
            DLScreen.setScreen(new TrainDebugScreen(null));
        });
    }

    @SuppressWarnings("resource")
    public static void initScheduleSectionInstruction(TravelSectionInstruction instruction, ModularGuiLineBuilder builder) {
        
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;

        ResizableButton btn = new ResizableButton(accessor.crn$getX(), accessor.crn$getY() - 4, 121, 16, TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + instruction.getId().getPath() + ".configure"), 
        (b) -> {
            if (Minecraft.getInstance().screen instanceof ScheduleScreen scheduleScreen) {
                ((ScheduleScreenAccessor)scheduleScreen).crn$getOnEditorClose().accept(true);
                builder.customArea(0, 0).speechBubble();
                Minecraft.getInstance().setScreen(new TrainSectionSettingsScreen(scheduleScreen, instruction.getData()));
            }
        }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                Graphics graphics = new Graphics(guiGraphics, guiGraphics.pose());
				DynamicGuiRenderer.renderArea(graphics, getX(), getY(), width, height, AreaStyle.GRAY, isActive() ? (isFocused() || isMouseOver(mouseX, mouseY) ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
                int j = isActive() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
                GuiUtils.drawString(graphics, Minecraft.getInstance().font, getX() + width / 2, getY() + (height - 8) / 2, this.getMessage(), j, EAlignment.CENTER, true);
            }
        };
		accessor.crn$getTarget().add(Pair.of(btn, "config_btn"));
    }

    public static void initResetTimingsInstruction(ResetTimingsInstruction instruction, ModularGuiLineBuilder builder) {
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        ResizableButton btn = new ResizableButton(accessor.crn$getX(), accessor.crn$getY() - 4, 16, 16, TextUtils.empty(), 
        (b) -> {
			Util.getPlatform().openUri(Constants.HELP_PAGE_SCHEDULED_TIMES_AND_REAL_TIME);
        }) {
			@Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                Graphics graphics = new Graphics(guiGraphics, guiGraphics.pose());
				DynamicGuiRenderer.renderArea(graphics, getX(), getY(), width, height, AreaStyle.GRAY, isActive() ? (isFocused() || isMouseOver(mouseX, mouseY) ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
				ModGuiIcons.HELP.render(graphics, getX(), getY());
            }
        };
		accessor.crn$getTarget().add(Pair.of(btn, "help_btn"));
    }

    public static void initDynamicDelayCondition(DynamicDelayCondition condition, ModularGuiLineBuilder builder) {
        
		builder.addScrollInput(0, 26, (i, l) -> {
			i.titled(Lang.translateDirect("generic.duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Value");

        builder.addScrollInput(26, 26, (i, l) -> {
			i.titled(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition." + condition.getId().getPath() + ".min_duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, DynamicDelayCondition.NBT_MIN);

		builder.addSelectionScrollInput(52, 58, (i, l) -> {
			i.forOptions(TimeUnit.translatedOptions())
				.titled(Lang.translateDirect("generic.timeUnit"));
		}, "TimeUnit");

		
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        ResizableButton btn = new ResizableButton(accessor.crn$getX() + 110, accessor.crn$getY() - 4, 16, 16, TextUtils.empty(), 
        (b) -> {
			Util.getPlatform().openUri(Constants.HELP_PAGE_DYNAMIC_DELAYS);
        }) {
			@Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                Graphics graphics = new Graphics(guiGraphics, guiGraphics.pose());
				DynamicGuiRenderer.renderArea(graphics, getX(), getY(), width, height, AreaStyle.GRAY, isActive() ? (isFocused() || isMouseOver(mouseX, mouseY) ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
				ModGuiIcons.HELP.render(graphics, getX(), getY());
            }
        };
		accessor.crn$getTarget().add(Pair.of(btn, "help_btn"));
    }
}
