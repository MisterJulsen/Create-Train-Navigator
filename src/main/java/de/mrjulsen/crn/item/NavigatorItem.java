package de.mrjulsen.crn.item;

import com.simibubi.create.AllCreativeModeTabs;

import de.mrjulsen.crn.client.ClientWrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NavigatorItem extends Item {

    public NavigatorItem() {
        super(new Properties().tab(AllCreativeModeTabs.BASE_CREATIVE_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) {
            ClientWrapper.showNavigatorGui(pLevel);
        }        
        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
