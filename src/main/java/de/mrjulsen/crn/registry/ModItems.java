package de.mrjulsen.crn.registry;

import com.tterrag.registrate.util.entry.ItemEntry;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.item.NavigatorItem;

public class ModItems {

    static {
		ModMain.REGISTRATE.creativeModeTab(() -> ModCreativeModeTab.MAIN);
	}

    public static final ItemEntry<NavigatorItem> NAVIGATOR = ModMain.REGISTRATE.item("navigator", NavigatorItem::new)
			.properties(p -> p.stacksTo(1))
			.register();
    
    public static void register() {
    }
}
