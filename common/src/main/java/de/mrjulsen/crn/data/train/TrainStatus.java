package de.mrjulsen.crn.data.train;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.Single;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class TrainStatus {

    public static final MutableComponent textOperationalDisruption = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".delay_reason.operational_disruption"); // Betriebsstörung
    public static final MutableComponent textTooFewTracks = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".delay_reason.too_few_tracks"); // Verfügbarkeit der Gleise eingeschränkt
    public static final MutableComponent textOperationalStabilization = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".delay_reason.operational_stabilization"); // Betriebsstabilisierung
    public static final MutableComponent textStaffShortage = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".delay_reason.staff_shortage"); // Kurzfristiger Personalausfall
    public static final MutableComponent textTrackClosed = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".delay_reason.track_closed");

    private static final Registry REGISTRY = Registry.create(CreateRailwaysNavigator.MOD_ID);
    public static final TrainStatus DEFAULT_DELAY = REGISTRY.registerDefault("default_delay", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.unknown_delay"), null));
    public static final TrainStatus DELAY_FROM_PREVIOUS_JOURNEY = REGISTRY.registerDefault("delay_from_previous_journey", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.delay_previous_journey"), null));
    public static final TrainStatus CANCELLED = REGISTRY.registerDefault("cancelled", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.cancelled"), null));
    
    public static final int HEIGHT = 9;

    private final TrainStatusType importance;
    private final TrainStatusCategory category;
    private final Function<TrainData, MutableComponent> text;
    //private final Function<NewTrainData, MutableComponent> reason;
    private final Predicate<TrainData> trigger;

    private ResourceLocation location;

    public TrainStatus(TrainStatusCategory category, TrainStatusType importance, Function<TrainData, MutableComponent> text, /*Function<NewTrainData, MutableComponent> reason,*/ Predicate<TrainData> trigger) {
        this.importance = importance;
        this.category = category;
        this.text = text;
        //this.reason = reason;
        this.trigger = trigger;
    }

    public TrainStatusType getImportance() {
        return importance;
    }

    public MutableComponent getText(TrainData data) {
        return text.apply(data);
    }
    
    /*
    public MutableComponent getReason(NewTrainData data) {
        return reason.apply(data);
    }
    */

    public boolean isTriggerd(TrainData data) {
        return trigger != null && trigger.test(data);
    }

    public CompiledTrainStatus compile(TrainData data) {
        return new CompiledTrainStatus(category, importance, getText(data));//, getReason(data));
    }

    public ResourceLocation getLocation() {
        return location;
    }
    

    private void setLocation(ResourceLocation location) {
        this.location = location;
    }


    public static record CompiledTrainStatus(TrainStatusCategory category, TrainStatusType type, Component text/*, Component reason*/) {

        public static final String NBT_CATEGORY = "Category";
        public static final String NBT_TYPE = "Type";
        public static final String NBT_TEXT = "Text";
        public static final String NBT_REASON = "Reason";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putByte(NBT_CATEGORY, category().getIndex());
            nbt.putByte(NBT_TYPE, type().getIndex());
            nbt.putString(NBT_TEXT, text().getString());            
            //nbt.putString(NBT_REASON, reason().getString());            
            return nbt;
        }

        public static CompiledTrainStatus fromNbt(CompoundTag nbt) {
            return new CompiledTrainStatus(
                TrainStatusCategory.getByIndex(nbt.getByte(NBT_CATEGORY)), 
                TrainStatusType.getByIndex(nbt.getByte(NBT_TYPE)),
                TextUtils.text(nbt.getString(NBT_TEXT))
                //TextUtils.text(nbt.getString(NBT_REASON))
            );
        }

        public int render(Graphics graphics, Single<Font> font, int x, int y, int maxWidth) {
            final int color = type().getColor();
            final float scale = 0.75f;
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(x, y, 0);
            GuiUtils.setTint(color);
            ModGuiIcons.IMPORTANT.render(graphics, -4, -3);
            graphics.poseStack().scale(scale, scale, 1);
            int height = (int)(ClientWrapper.renderMultilineLabelSafe(graphics, (int)(10 / scale), (int)(2 / scale), font.getFirst(), text(), (int)(maxWidth / scale), color) * scale);
            graphics.poseStack().popPose();
            
            return Math.max(HEIGHT, height + 2);
        }
    }


    public static class Registry {

        private static final Map<ResourceLocation, TrainStatus> registeredStatusInfos = new HashMap<>();

        private final String modid;

        private Registry(String modid) {
            this.modid = modid;
        }

        public static Registry create(String modid) {
            return new Registry(modid);
        }

        public static ImmutableMap<ResourceLocation, TrainStatus> getRegisteredStatus() {
            return ImmutableMap.copyOf(registeredStatusInfos);
        }

        public TrainStatus register(String name, TrainStatus statusPattern) {
            ResourceLocation loc = new ResourceLocation(modid, name);
            statusPattern.setLocation(loc);
            registeredStatusInfos.put(loc, statusPattern);
            return statusPattern;
        }

        public TrainStatus registerDefault(String name, TrainStatus statusPattern) {
            ResourceLocation loc = new ResourceLocation(modid, name);
            statusPattern.setLocation(loc);
            registeredStatusInfos.put(loc, statusPattern);
            return statusPattern;
        }

        public TrainStatus unregister(ResourceLocation location) {
            return registeredStatusInfos.remove(location);
        }

        public TrainStatus get(ResourceLocation location) {
            return registeredStatusInfos.get(location);
        }
        
        public boolean isRegistered(ResourceLocation location) {
            return registeredStatusInfos.containsKey(location);
        }

        public void delete(String modid) {
            registeredStatusInfos.keySet().removeIf(x -> x.getNamespace().equals(modid));
        }
    }

    public static enum TrainStatusType {
        MESSAGE_DEFAULT((byte)0, 0xFFFFFFFF),
        MESSAGE_WARN((byte)1, ChatFormatting.GOLD.getColor()),
        MESSAGE_IMPORTANT((byte)2, Constants.COLOR_DELAYED),
        DELAY((byte)3, Constants.COLOR_DELAYED);

        private final byte index;
        private final int color;

        private TrainStatusType(byte index, int color) {
            this.index = index;
            this.color = color;
        }

        public byte getIndex() {
            return index;
        }

        public int getColor() {
            return color;
        }

        public static TrainStatusType getByIndex(int index) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(MESSAGE_DEFAULT);
        }
    }

    public static enum TrainStatusCategory {
        /** Information about a train. */
        TRAIN((byte)0),
        /** Information about a single station. */
        STATION((byte)1);

        private final byte index;

        private TrainStatusCategory(byte index) {
            this.index = index;
        }

        public byte getIndex() {
            return index;
        }

        public static TrainStatusCategory getByIndex(int index) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(TRAIN);
        }
    }
}
