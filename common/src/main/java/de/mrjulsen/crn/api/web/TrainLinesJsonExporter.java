package de.mrjulsen.crn.api.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;

/** Exports train line and category colors from global settings. */
public final class TrainLinesJsonExporter {

    private TrainLinesJsonExporter() {}

    public static JsonObject buildRoot(long worldTick) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", worldTick);

        JsonArray lines = new JsonArray();
        JsonArray categories = new JsonArray();

        if (GlobalSettings.hasInstance()) {
            for (TrainLine line : GlobalSettings.getInstance().getAllTrainLines()) {
                lines.add(WebApiJsonHelper.lineObject(line));
            }
            for (TrainCategory category : GlobalSettings.getInstance().getAllTrainCategories()) {
                categories.add(WebApiJsonHelper.categoryObject(category));
            }
        }

        root.add("lines", lines);
        root.addProperty("lineCount", lines.size());
        root.add("categories", categories);
        root.addProperty("categoryCount", categories.size());
        return root;
    }
}
