package de.mrjulsen.crn.client.gui;

import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import net.minecraft.resources.ResourceLocation;

public enum ModGuiIcons {
    EMPTY("empty", 0, 0),
    CHECK("check", 1, 0),
    CROSS("cross", 2, 0),
    WARN("warn", 3, 0),
    SETTINGS("settings", 0, 1),
    FILTER("filter", 1, 1),
    BOOKMARK("bookmark", 2, 1),
    PIN("pin", 3, 1),
    UNPIN("unpin", 4, 1),
    SOUND_ON("sound_on", 5, 1),
    SOUND_OFF("sound_off", 6, 1),
    SCALE("scale", 7, 1),
    TOP_LEFT("top_left", 8, 1),
    TOP_RIGHT("top_right", 9, 1),
    BOTTOM_LEFT("bottom_left", 10, 1),
    BOTTOM_RIGHT("bottom_right", 11, 1),
    POSITION("position", 12, 1),
    TARGET("target", 13, 1),
    TIME("target", 14, 1),
    WALK("walk", 15, 1),
    INFO("info", 0, 2),
    EXPAND("expand", 1, 2),
    COLLAPSE("collapse", 2, 2),
    DELETE("delete", 3, 2),
    ADD("add", 4, 2);

    private String id;
    private int u;
    private int v;

    public static final int ICON_SIZE = 16;
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(ModMain.MOD_ID, "textures/gui/icons.png");;

    ModGuiIcons(String id, int u, int v) {
        this.id = id;
        this.u = u;
        this.v = v;
    }

    public String getId() {
        return id;
    }

    public int getUMultiplier() {
        return u;
    }

    public int getVMultiplier() {
        return v;
    }

    public int getU() {
        return u * ICON_SIZE;
    }

    public int getV() {
        return v * ICON_SIZE;
    }

    public static ModGuiIcons getByStringId(String id) {
        return Arrays.stream(values()).filter(x -> x.getId().equals(id)).findFirst().orElse(ModGuiIcons.EMPTY);
    }

    public AllIcons getAsCreateIcon() {
        return new ModAllIcons(u, v);
    }

    public void render(PoseStack poseStack, int x, int y) {
        GuiUtils.blit(ModGuiIcons.ICON_LOCATION, poseStack, x, y, getU(), getV(), ICON_SIZE, ICON_SIZE);
    }

    public static class ModAllIcons extends AllIcons {

        public ModAllIcons(int x, int y) {
            super(x, y);
        }

        @Override
        public void bind() {
            RenderSystem.setShaderTexture(0, ICON_LOCATION);
        }
        
    }
}
