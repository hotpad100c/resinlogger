package mypals.ml.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mypals.ml.ResinLogger;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.block.MultifaceBlock.getProperty;
import static net.minecraft.block.MultifaceBlock.hasDirection;

@Mixin(MultifaceBlock.class)
public class MuItifaceBlockMixin {
	@Final
	@Shadow
	static Direction[] DIRECTIONS;
	@WrapMethod(method = "getStateForNeighborUpdate")
	private BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random, Operation<BlockState> original) {

		if(world.getBlockState(pos).getBlock() instanceof MultifaceBlock resin && world.getBlockState(pos).getBlock().equals(Blocks.RESIN_CLUMP) && world instanceof ServerWorld serverWorld && serverWorld.getGameRules().getBoolean(ResinLogger.RESIN_DROP_FIX)) {
			if (!hasAnyDirection_resin(state)) {
				return Blocks.AIR.getDefaultState();
			} else {
				return hasDirection(state, direction) && !resin.canGrowOn(world, direction, neighborPos, neighborState) ? disableDirection_resin(state, direction,getProperty(direction), serverWorld, pos) : state;
			}
		}else{
			return original.call(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		}
	}
	private static BlockState disableDirection_resin(BlockState state, Direction dir, BooleanProperty direction, ServerWorld world, BlockPos pos) {
		BlockState blockState = state.with(direction, false);
		if(hasAnyDirection_resin(blockState)){
			MultifaceBlock.dropStack(world,pos,dir.getOpposite(),Blocks.RESIN_CLUMP.asItem().getDefaultStack());
			return blockState;
		}else{
			return Blocks.AIR.getDefaultState();
		}
	}
	private static boolean hasAnyDirection_resin(BlockState state) {
		Direction[] var1 = DIRECTIONS;
		int var2 = var1.length;

		for(int var3 = 0; var3 < var2; ++var3) {
			Direction direction = var1[var3];
			if (hasDirection(state, direction)) {
				return true;
			}
		}
		return false;
	}
}