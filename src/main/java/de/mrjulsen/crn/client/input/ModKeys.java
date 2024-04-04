package de.mrjulsen.crn.client.input;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public class ModKeys {
    
    private ModKeys() {}

    public static KeyMapping keyRouteOverlayOptions;

    public static KeyMapping registerKey(String name, String category, IKeyConflictContext context, KeyModifier modifier, int keycode) {
        KeyMapping k = new KeyMapping("key." + ModMain.MOD_ID + "." + name, context, modifier, InputConstants.Type.KEYSYM, keycode, "key." + ModMain.MOD_ID + ".category." + category);
        return k;
    }
}
