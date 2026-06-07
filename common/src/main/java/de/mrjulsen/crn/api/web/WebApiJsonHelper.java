package de.mrjulsen.crn.api.web;

import java.util.List;

import com.google.gson.JsonObject;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class WebApiJsonHelper {

    private WebApiJsonHelper() {}

    public static JsonObject colorObject(DLColor color) {
        JsonObject obj = new JsonObject();
        if (color == null || color.isTransparent()) {
            obj.addProperty("transparent", true);
            return obj;
        }
        int argb = color.getAsARGB();
        obj.addProperty("argb", argb);
        obj.addProperty("hex", formatHex(argb));
        obj.addProperty("r", (argb >> 16) & 0xFF);
        obj.addProperty("g", (argb >> 8) & 0xFF);
        obj.addProperty("b", argb & 0xFF);
        obj.addProperty("a", (argb >> 24) & 0xFF);
        return obj;
    }

    public static JsonObject lineObject(TrainLine line) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", line.getId().toString());
        obj.addProperty("name", line.getLineName());
        obj.add("color", colorObject(line.getColor()));
        return obj;
    }

    public static JsonObject categoryObject(TrainCategory category) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", category.getId().toString());
        obj.addProperty("name", category.getCategoryName());
        obj.add("color", colorObject(category.getColor()));
        return obj;
    }

    public static void addTrainInfo(JsonObject target, TrainInfo info) {
        if (info == null) {
            return;
        }
        if (info.line() != null) {
            target.add("line", lineObject(info.line()));
            target.addProperty("lineId", info.line().getId().toString());
            target.addProperty("lineName", info.line().getLineName());
        }
        if (info.category() != null) {
            target.add("category", categoryObject(info.category()));
            target.addProperty("categoryId", info.category().getId().toString());
            target.addProperty("categoryName", info.category().getCategoryName());
        }
    }

    public static void addLineFromSection(JsonObject target, TrainData data) {
        data.getCurrentSection().getTrainLine().ifPresent(line -> {
            target.add("line", lineObject(line));
            target.addProperty("lineId", line.getId().toString());
            target.addProperty("lineName", line.getLineName());
        });
        data.getCurrentSection().getTrainCategory().ifPresent(category -> {
            target.add("category", categoryObject(category));
            target.addProperty("categoryId", category.getId().toString());
            target.addProperty("categoryName", category.getCategoryName());
        });
    }

    public static void addPosition(JsonObject target, Train train) {
        if (train == null) {
            return;
        }
        List<ResourceKey<Level>> dimensions = train.getPresentDimensions();
        if (dimensions.isEmpty()) {
            return;
        }
        ResourceKey<Level> dimension = dimensions.get(0);
        BlockPos pos = train.getPositionInDimension(dimension).orElse(null);
        if (pos == null) {
            return;
        }
        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());
        ResourceLocation dimId = dimension.location();
        position.addProperty("dimension", dimId.toString());
        target.add("position", position);
    }

    public static void addNextStopDistance(JsonObject target, TrainData data, boolean atStation) {
        Train train = data.getTrain();
        if (train == null || atStation) {
            return;
        }
        if (train.navigation != null && train.navigation.destination != null) {
            double blocks = train.navigation.distanceToDestination;
            if (blocks > 0) {
                target.addProperty("blocksToNextStop", blocks);
            }
        }
        target.addProperty("ticksToNextStop", Math.max(0, data.ticksToNextStop));
        if (data.ticksToNextStop > 0) {
            target.addProperty("secondsToNextStop", data.ticksToNextStop / 20);
        }
    }

    private static String formatHex(int argb) {
        return "#" + String.format("%08X", argb);
    }
}
