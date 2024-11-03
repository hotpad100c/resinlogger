package mypals.ml.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import mypals.ml.ResinLogger;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CreakingHeartBlockEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.spongepowered.asm.mixin.injection.At.Shift.BEFORE;

//Useless :)
//Useless :)
//Useless :)
//Useless :)
@Mixin(CreakingHeartBlockEntity.class)
public class CreakingHeartBlockEntityMixin {
	@Inject(at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos;iterateRecursively(Lnet/minecraft/util/math/BlockPos;IILjava/util/function/BiConsumer;Ljava/util/function/Function;)I",
			shift=BEFORE),
			method = "findResinGenerationPos")
	private void startedSpawnResin(CallbackInfoReturnable<Optional<BlockPos>> cir, @Local BlockPos blockPos) {
		Text position = Text.literal(" " + blockPos + " ").formatted(Formatting.BLUE).formatted(Formatting.BOLD);
		//ResinLogger.log(Text.literal("Creaking heart at " + position.getLiteralString() + " started to spawn resin now..."),false);
	}
	/*@Redirect(at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/math/BlockPos;iterateRecursively(Lnet/minecraft/util/math/BlockPos;IILjava/util/function/BiConsumer;Ljava/util/function/Function;)I"),
			method = "findResinGenerationPos")
	private int spawnResin(BlockPos pos, int maxDepth, int maxIterations, BiConsumer<BlockPos, Consumer<BlockPos>> nextQueuer, Function<BlockPos, BlockPos.IterationState> callback) {
		Queue<Pair<BlockPos, Integer>> queue = new ArrayDeque();
		LongSet longSet = new LongOpenHashSet();
		queue.add(Pair.of(pos, 0));
		int i = 0;

		while (!queue.isEmpty()) {
			Pair<BlockPos, Integer> pair = (Pair<BlockPos, Integer>)queue.poll();
			BlockPos blockPos2 = pair.getLeft();
			int j = pair.getRight();
			long l = blockPos2.asLong();
			if (longSet.add(l)) {
				BlockPos.IterationState iterationState = getBlockIterationState()

				if (iterationState != BlockPos.IterationState.SKIP) {
					if (iterationState == BlockPos.IterationState.STOP) {
						break;
					}

					if (++i >= maxIterations) {
						return i;
					}

					if (j < maxDepth) {
						nextQueuer.accept(blockPos2, (Consumer)queuedPos -> queue.add(Pair.of((BlockPos)queuedPos, j + 1)));
					}
				}
			}
		}

		return i;
	}
	public BlockPos.IterationState getBlockIterationState(World world, BlockPos pos, Consumer<BlockPos> consumer){
		for (Direction direction : Util.copyShuffled(Direction.values(), world.random)) {
			BlockPos blockPosx = pos.offset(direction);
			BlockState blockState = world.getBlockState(blockPosx);
			if (blockState.isIn(BlockTags.PALE_OAK_LOGS)) {
				consumer.accept(blockPosx);
			}
		}
	}*/
}