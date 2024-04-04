package de.mrjulsen.crn.registry;

import com.tterrag.registrate.util.entry.ItemEntry;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.item.NavigatorItem;

public class ModItems {

    static {
		ModMain.REGISTRATE.setCreativeTab(ModCreativeModeTab.MAIN_TAB);
	}

    public static final ItemEntry<NavigatorItem> NAVIGATOR = ModMain.REGISTRATE.item("navigator", NavigatorItem::new)
			.properties(p -> p.stacksTo(1))
            .tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.register();
    
    public static void register() {
    }
}
