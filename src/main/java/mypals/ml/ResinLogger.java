package mypals.ml;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.entity.CreakingHeartBlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ResinLogger implements ModInitializer {
	public static final String MOD_ID = "resinlogger";
	public static MinecraftServer server;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.BooleanRule> RESIN_DROP_FIX =
			GameRuleRegistry.register("multifaceResinDropIssueFix", GameRules.Category.DROPS, GameRuleFactory.createBooleanRule(true));
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ResinLogger.server = server;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommand(dispatcher);
		});

		LOGGER.info("Hello Fabric world!");
	}

	private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		// 注册命令 `/broadcast`
		dispatcher.register(CommandManager.literal("simulateResinGeneration")
				.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						.executes(
								context -> {
									findResinGenerationPos(context.getSource().getWorld(),
											BlockPosArgumentType.getBlockPos(context,"pos"),
											-1,
											false);
									return 1;
								})
						.then(CommandManager.argument("resultOnly", BoolArgumentType.bool())
								.executes(
										context -> {
											findResinGenerationPos(context.getSource().getWorld(),
													BlockPosArgumentType.getBlockPos(context,"pos"),
													-1,
													BoolArgumentType.getBool(context,"resultOnly"));
											return 1;
										})
						)
						.then(CommandManager.argument("count", IntegerArgumentType.integer())
								.executes(
										context -> {
									findResinGenerationPos(context.getSource().getWorld(),
											BlockPosArgumentType.getBlockPos(context,"pos"),
											IntegerArgumentType.getInteger(context,"count"),
											false);
											return 1;
										})
								.then(CommandManager.argument("resultOnly", BoolArgumentType.bool())
									.executes(
											context -> {
										findResinGenerationPos(context.getSource().getWorld(),
												BlockPosArgumentType.getBlockPos(context,"pos"),
												IntegerArgumentType.getInteger(context,"count"),
												BoolArgumentType.getBool(context,"resultOnly"));
												return 1;
											})
								)
						)
				)
		);
		dispatcher.register(CommandManager.literal("simulateResinGenerationForCreakingHeartsInArea")
				.then(CommandManager.argument("pos1", BlockPosArgumentType.blockPos())
						.then(CommandManager.argument("pos2", BlockPosArgumentType.blockPos())
								.executes(context -> {
									ServerCommandSource source = context.getSource();
									World world = source.getWorld();
									BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
									BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
									findResinGenerationPosArea(world, pos1, pos2, -1, true);

									return 1;
								})
								.then(CommandManager.argument("resultOnly", BoolArgumentType.bool())
										.executes(context -> {
											ServerCommandSource source = context.getSource();
											World world = source.getWorld();
											BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
											BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
											int count = -1;
											Boolean resultOnly = BoolArgumentType.getBool(context,"resultOnly");
											findResinGenerationPosArea(world, pos1, pos2, count, resultOnly);
											return 1;
										})
								)
								.then(CommandManager.argument("count", IntegerArgumentType.integer())
										.executes(context -> {
											ServerCommandSource source = context.getSource();
											World world = source.getWorld();
											BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
											BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
											int count = IntegerArgumentType.getInteger(context,"count");

											findResinGenerationPosArea(world, pos1, pos2, count, true);

											return 1;
										})
										.then(CommandManager.argument("resultOnly", BoolArgumentType.bool())
												.executes(context -> {
													ServerCommandSource source = context.getSource();
													World world = source.getWorld();
													BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
													BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
													int count = IntegerArgumentType.getInteger(context,"count");
													Boolean resultOnly = BoolArgumentType.getBool(context,"resultOnly");
													findResinGenerationPosArea(world, pos1, pos2, count, resultOnly);
													return 1;
												})
										)
								)
						)
				)
		);
	}


	public static void log(World world, Text text, boolean overlay, boolean display) {
		if(!display){return;}
		world.getPlayers().forEach(player -> player.sendMessage(text, overlay));

	}
	private void findResinGenerationPosArea(World world, BlockPos pos1, BlockPos pos2, int times, boolean resultOnly) {
		int minX = Math.min(pos1.getX(), pos2.getX());
		int minY = Math.min(pos1.getY(), pos2.getY());
		int minZ = Math.min(pos1.getZ(), pos2.getZ());
		int maxX = Math.max(pos1.getX(), pos2.getX());
		int maxY = Math.max(pos1.getY(), pos2.getY());
		int maxZ = Math.max(pos1.getZ(), pos2.getZ());

		int successCount = 0;
		int faildCount = 0;

		ArrayList<BlockPos> hearts = new ArrayList<>();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos currentPos = new BlockPos(x, y, z);
					if(world.getBlockEntity(currentPos) instanceof CreakingHeartBlockEntity) {
						hearts.add(currentPos);
					}
				}
			}
		}
		for(Object heartPos : Util.copyShuffled(hearts.toArray(), world.random)) {
			ResinSpawnData data = (findResinGenerationPos(world, (BlockPos) heartPos, times, resultOnly));
			successCount += data.success;
			faildCount += data.failed;
		}
		int total = successCount+faildCount;
		log(world, Text.translatable("log.generation_summary",total, successCount, faildCount).formatted(Formatting.ITALIC), false, true);
	}
	private ResinSpawnData findResinGenerationPos(World world, BlockPos p, int times, Boolean resultOnly) {
		ResinSpawnData data = new ResinSpawnData();
		for(int i = 0; i < (times > 0? times : world.random.nextBetween(2,3));i++) {
			BlockPos blockPos = p;

			Mutable<BlockPos> mutable = new MutableObject<>(null);
			log(world, Text.translatable("log.detect_start", blockPos.toString()).formatted(Formatting.ITALIC), false, !resultOnly);

			BlockPos.iterateRecursively(blockPos, 2, 64, (pos, consumer) -> {
				log(world, Text.translatable("log.check_surrounding_blocks", pos.toString()).formatted(Formatting.ITALIC), false, !resultOnly);

				boolean successed = false;
				for (Direction direction : Util.copyShuffled(Direction.values(), world.random)) {
					BlockPos blockPosx = pos.offset(direction);
					BlockState blockState = world.getBlockState(blockPosx);
					if (blockState.isIn(BlockTags.PALE_OAK_LOGS)) {
						successed = true;
						log(world, Text.translatable("log.success_block_found", blockPosx.toString()).formatted(Formatting.DARK_GREEN), false, !resultOnly);
						consumer.accept(blockPosx);
					}
				}
				if (!successed) {
					log(world, Text.translatable("log.failure_no_blocks_found", pos.toString()).formatted(Formatting.RED), false, !resultOnly);
				}
			}, pos -> {
				if (!world.getBlockState(pos).isIn(BlockTags.PALE_OAK_LOGS)) {
					log(world, Text.translatable("log.not_part_of_tag", pos.toString(), world.getBlockState(pos).toString()).formatted(Formatting.YELLOW), false, !resultOnly);
					log(world, Text.translatable("log.separator").formatted(Formatting.BOLD), false, !resultOnly);
					return BlockPos.IterationState.ACCEPT;
				} else {
					for (Direction direction : Util.copyShuffled(Direction.values(), world.random)) {
						BlockPos blockPosx = pos.offset(direction);
						BlockState blockState = world.getBlockState(blockPosx);
						Direction direction2 = direction.getOpposite();
						if (blockState.isAir()) {
							blockState = Blocks.RESIN_CLUMP.getDefaultState();
							log(world, Text.translatable("log.air_block_found", blockPosx.toString()).formatted(Formatting.DARK_GREEN), false, !resultOnly);
						}

						if (blockState.isOf(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasDirection(blockState, direction2)) {
							world.setBlockState(blockPosx, blockState.with(MultifaceBlock.getProperty(direction2), true), Block.NOTIFY_ALL);
							log(world, Text.translatable("log.resin_generated", blockPosx.toString(), world.getBlockState(blockPosx).toString()).formatted(Formatting.GREEN), false, !resultOnly);
							mutable.setValue(blockPosx);
							return BlockPos.IterationState.STOP;
						}
						log(world, Text.translatable("log.block_prevented_generation", blockPosx.toString(), world.getBlockState(blockPosx).toString()).formatted(Formatting.YELLOW), false, !resultOnly);
					}
					log(world, Text.translatable("log.attempt_separator").formatted(Formatting.BOLD), false, !resultOnly);
					return BlockPos.IterationState.ACCEPT;
				}
			});

			if (mutable.getValue() == null) {
				data.failed++;
				log(world, Text.translatable("log.generation_failed").formatted(Formatting.RED).formatted(Formatting.BOLD), false,true);
			}else{
				data.success++;
				log(world,Text.translatable("log.generation_success").formatted(Formatting.GREEN).formatted(Formatting.BOLD), false, resultOnly);
			}
		}
		return data;
	}


}