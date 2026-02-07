package de.mrjulsen.crn.client;

import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.SectionPos;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition.TimeUnit;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.client.Screens;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.NavigatorToast;
import de.mrjulsen.crn.client.gui.widgets.vanilla.ResizableButton;
import de.mrjulsen.crn.client.gui.windows.AdvancedDisplaySettingsWindow;
import de.mrjulsen.crn.client.gui.windows.PrioritizedDestinationInstructionSettingsWindow;
import de.mrjulsen.crn.client.gui.windows.TrainSectionSettingsWindow;
import de.mrjulsen.crn.client.gui.windows.TrainSeparationSettingsWindow;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.data.schedule.instruction.ResetTimingsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import de.mrjulsen.crn.item.NavigatorItem;
import de.mrjulsen.crn.mixin.ModularGuiLineBuilderAccessor;
import de.mrjulsen.crn.mixin.ScheduleScreenAccessor;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacketData;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClientWrapper {
    
    public static final ModelResourceLocation NAVIGATOR_WORLD_MODEL = new ModelResourceLocation(CreateRailwaysNavigator.MOD_ID, "navigator_world", "inventory");
    
    private static CustomLanguage currentLanguage;
    private static Language currentClientLanguage;
    
    public static void showNavigatorGui() {
        Screens.showNavigatorScreen(null, false);
    }


    public static void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }

    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    public static void handleErrorMessagePacket(ServerErrorPacketData packet, NetworkPacketContext ctx) {        
        Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, Constants.TEXT_SERVER_ERROR, TextUtils.text(packet.message)));   
    }
    
    public static void showAdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity, AbstractContraptionEntity contraption) {
        DLWindow.openWindow(mgr -> new AdvancedDisplaySettingsWindow(mgr, blockEntity, contraption));
    }

    public static void updateLanguage(CustomLanguage lang, boolean force) {
        if (currentLanguage == lang && !force) {
            return;
        }

        LanguageInfo info = lang == CustomLanguage.DEFAULT ? null : Minecraft.getInstance().getLanguageManager().getLanguage(lang.getCode());
        if (info == null) {
            info = Minecraft.getInstance().getLanguageManager().getLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
        }
        currentLanguage = lang;
        if (lang == CustomLanguage.DEFAULT || info == null) {
            currentClientLanguage = Language.getInstance();
            CreateRailwaysNavigator.LOGGER.info("Updated custom language to: (Default)");
        } else {
            currentClientLanguage = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(lang == CustomLanguage.DEFAULT ? Minecraft.getInstance().getLanguageManager().getSelected() : lang.getCode()), false);
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

    public static int renderMultilineLabelSafe(DLGuiGraphics graphics, int x, int y, Font font, Component text, int maxWidth, DLColor color) {
        MultiLineLabel label = MultiLineLabel.create(font, text, maxWidth);
        label.renderLeftAlignedNoShadow(graphics.graphics(), x, y, font.lineHeight, color.getAsARGB());
        return font.lineHeight * label.getLineCount();
    }

    public static int getTextBlockHeight(Font font, Component text, int maxWidth) {
        int lines = font.split(text, maxWidth).size();
        return lines * font.lineHeight;
    }

    public static void showTrainDebugScreen() {
        RenderSystem.recordRenderCall(() -> {
            //DLScreen.setScreen(new TrainDebugScreen(null));
        });
    }

    public static void initPrioritizedDestinationInstruction(PrioritizedDestinationInstruction instruction, ModularGuiLineBuilder builder) {
        
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;

        ResizableButton btn = new ResizableButton(accessor.crn$getX(), accessor.crn$getY() - 4, 121, 16, TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.configure"), 
        (b) -> {
            if (Minecraft.getInstance().screen instanceof ScheduleScreen scheduleScreen) {
                ((ScheduleScreenAccessor)scheduleScreen).crn$getOnEditorClose().accept(true);
                builder.customArea(0, 0).speechBubble();
                DLWindow.openWindow(mgr -> new PrioritizedDestinationInstructionSettingsWindow(mgr, instruction, instruction.getData()));
            }
        }) {
            /*
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                Graphics graphics = new Graphics(guiGraphics, guiGraphics.pose());
				DynamicGuiRenderer.renderArea(graphics, getX(), getY(), width, height, AreaStyle.GRAY, isActive() ? (isFocused() || isMouseOver(mouseX, mouseY) ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
                int j = isActive() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
                GuiUtils.drawString(graphics, Minecraft.getInstance().font, getX() + width / 2, getY() + (height - 8) / 2, this.getMessage(), j, ETextAlignment.CENTER, true);
            }
                */
        };
		accessor.crn$getTarget().add(Pair.of(btn, "config_btn"));
    }

    public static void initScheduleSectionInstruction(TravelSectionInstruction instruction, ModularGuiLineBuilder builder) {
        
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        ResizableButton btn = new ResizableButton(accessor.crn$getX(), accessor.crn$getY() - 4, 121, 16, TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.configure"), 
        (b) -> {
            if (Minecraft.getInstance().screen instanceof ScheduleScreen scheduleScreen) {
                ((ScheduleScreenAccessor)scheduleScreen).crn$getOnEditorClose().accept(true);
                builder.customArea(0, 0).speechBubble();
                DLWindow.openWindow(mgr -> new TrainSectionSettingsWindow(mgr, instruction.getData()));
            }
        });
		accessor.crn$getTarget().add(Pair.of(btn, "config_btn"));
    }

    public static void initResetTimingsInstruction(ResetTimingsInstruction instruction, ModularGuiLineBuilder builder) {
        /*
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
        */
    }

    public static void initDynamicDelayCondition(DynamicDelayCondition condition, ModularGuiLineBuilder builder) {
        
        builder.addScrollInput(0, 26, (i, l) -> {
			i.titled(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition." + condition.getId().getPath() + ".min_duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, DynamicDelayCondition.NBT_MIN);

		builder.addScrollInput(26, 26, (i, l) -> {
			i.titled(CreateLang.translateDirect("generic.duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Value");

		builder.addSelectionScrollInput(52, 58, (i, l) -> {
			i.forOptions(TimeUnit.translatedOptions())
				.titled(CreateLang.translateDirect("generic.timeUnit"));
		}, "TimeUnit");

		
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        /*
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
        */
    }

    @SuppressWarnings("resource")
    public static void initTimingAdjustmentGui(TrainSeparationCondition condition, ModularGuiLineBuilder builder) {

        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        ResizableButton btn = new ResizableButton(accessor.crn$getX(), accessor.crn$getY() - 4, 121, 16, TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.configure"), 
        (b) -> {
            if (Minecraft.getInstance().screen instanceof ScheduleScreen scheduleScreen) {
                ((ScheduleScreenAccessor)scheduleScreen).crn$getOnEditorClose().accept(true);
                builder.customArea(0, 0).speechBubble();
                DLWindow.openWindow(mgr -> new TrainSeparationSettingsWindow(mgr, condition.getData()));
            }
        });
		accessor.crn$getTarget().add(Pair.of(btn, "config_btn"));
    }

    public static void renderNavigatorItem(DLGraphics graphics, ItemStack itemStack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIXED) {
            return;
        }
        

        int backgroundId = itemStack.getOrCreateTag().getInt(NavigatorItem.NBT_BACKGROUND_ID);
        DLTime time = DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem());

        Font font = Minecraft.getInstance().font;
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.translate(4, 2, -1.26f);
        RenderUtils.renderTexture(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, String.format("textures/item/navigator_backgrounds/%s.png", backgroundId)), graphics, new Vector3f(0), 8, 12, 0, 0, 1F / 12F * 8, 1F, Direction.UP, DLColor.WHITE, LightTexture.FULL_BRIGHT, false);
        
        poseStack.translate(0, 0, -0.01f);
        poseStack.pushPose();
        poseStack.translate(4, 0.8f, 0);
        poseStack.scale(0.075f, 0.075f, 0.075f);
        RenderUtils.drawString(graphics, font, 0, 0, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.date", (long)time.toGameDays()), DLColor.WHITE, ETextAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        
        poseStack.pushPose();
        poseStack.translate(4, 2, 0);
        poseStack.scale(0.2f, 0.2f, 0.2f);
        RenderUtils.drawString(graphics, font, 0, 0, time.format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME), DLColor.WHITE, ETextAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    public static Owner getMe() {
        return new Owner(Minecraft.getInstance().player);
    }

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
