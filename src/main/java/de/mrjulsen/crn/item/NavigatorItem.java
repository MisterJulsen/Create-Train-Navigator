package de.mrjulsen.crn.item;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.ModCreativeModeTab;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NavigatorItem extends Item {

    public NavigatorItem() {
        super(new Properties().tab(ModCreativeModeTab.MOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) {
            ClientWrapper.showNavigatorGui(pLevel);
        }        
        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
