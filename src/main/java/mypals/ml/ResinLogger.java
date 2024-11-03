package mypals.ml;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Optional;

public class ResinLogger implements ModInitializer {
	public static final String MOD_ID = "resinlogger";
	private static MinecraftServer server;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
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
		dispatcher.register(CommandManager.literal("simulateSpawnResin")
				.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						.executes(
								context -> {
									return findResinGenerationPos(context.getSource().getWorld(),
											BlockPosArgumentType.getBlockPos(context,"pos"),
											1,
											false);
								})
						.then(CommandManager.argument("count", IntegerArgumentType.integer())
								.executes(
										context -> {
									return findResinGenerationPos(context.getSource().getWorld(),
											BlockPosArgumentType.getBlockPos(context,"pos"),
											IntegerArgumentType.getInteger(context,"count"),
											false);
								})
								.then(CommandManager.argument("resultOnly", BoolArgumentType.bool())
									.executes(
											context -> {
										return findResinGenerationPos(context.getSource().getWorld(),
												BlockPosArgumentType.getBlockPos(context,"pos"),
												IntegerArgumentType.getInteger(context,"count"),
												BoolArgumentType.getBool(context,"resultOnly"));
									})
								)
						)
				)
		);
	}


	public static void log(World world, Text text, boolean overlay, boolean display) {
		if(server == null){return;}
		if(!display){return;};
		world.getPlayers().forEach(player -> player.sendMessage(text, overlay));

	}
	private int findResinGenerationPos(World world, BlockPos p,int times, Boolean resultOnly) {
		for(int i = 0; i<times;i++) {
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
				log(world, Text.translatable("log.generation_failed").formatted(Formatting.RED).formatted(Formatting.BOLD), false,true);
			}else{
				log(world,Text.translatable("log.generation_successed").formatted(Formatting.GREEN).formatted(Formatting.BOLD), false, resultOnly);
			}
		}
		return 1;
	}


}