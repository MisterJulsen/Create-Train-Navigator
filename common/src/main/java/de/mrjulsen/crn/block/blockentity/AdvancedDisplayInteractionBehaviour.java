package de.mrjulsen.crn.block.blockentity;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.block.IBlockGetter;
import de.mrjulsen.crn.client.ClientWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class AdvancedDisplayInteractionBehaviour extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        Contraption contraption = contraptionEntity.getContraption();  
        MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
        if (actor == null || actor.right == null)
            return false;
    
        MovementContext ctx = actor.getRight();
        Level level = ctx.world;

        if (CRNPlatformSpecific.getClientContraptionBlockEntity(contraption, localPos) instanceof AdvancedDisplayBlockEntity be && player.getItemInHand(activeHand).is(AllItems.WRENCH.get())) {
            AdvancedDisplayBlockEntity controller = be.getController(new IBlockGetter.ContraptionBlockGetter(contraptionEntity));
            if (controller != null) {
                ClientWrapper.showAdvancedDisplaySettingsScreen(controller, contraptionEntity);
            }
        }

        return true;
    }
}
