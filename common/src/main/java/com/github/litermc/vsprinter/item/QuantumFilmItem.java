package com.github.litermc.vsprinter.item;

import com.github.litermc.vsprinter.api.PrintableSchematic;
import com.github.litermc.vsprinter.api.SchematicManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.joml.primitives.AABBi;

import java.util.List;

public class QuantumFilmItem extends Item {
	private static final int MAX_BLOCK_LIMIT = 64 * 64 * 64;
	private static final String FIRST_POS_TAG = "FirstPos";
	private static final String SECOND_POS_TAG = "SecondPos";
	private static final String BLUEPRINT_TAG = "Blueprint";

	public QuantumFilmItem(final Item.Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(final UseOnContext ctx) {
		final Player player = ctx.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}

		BlockPos assigningPos = ctx.getClickedPos();
		if (ctx.getClickedFace().getStepY() != 0) {
			assigningPos = assigningPos.relative(ctx.getClickedFace());
		}

		return onSetPos(ctx.getItemInHand(), player.level(), player, assigningPos);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		final InteractionResult result = onSetPos(stack, level, player, player.blockPosition());
		return new InteractionResultHolder<>(result, stack);
	}

	protected InteractionResult onSetPos(final ItemStack stack, final Level level, final Player player, final BlockPos pos) {
		if (player.isShiftKeyDown()) {
			clearBlockPos(stack);
			player.displayClientMessage(
				Component.literal("Positions cleared"),
				true
			);
			return InteractionResult.SUCCESS;
		}

		final BlockPos firstPos = getBlockPos(stack, FIRST_POS_TAG);
		if (firstPos == null) {
			setBlockPos(stack, FIRST_POS_TAG, pos);
			player.displayClientMessage(
				Component.literal(String.format("First Pos Set - %d %d %d", pos.getX(), pos.getY(), pos.getZ())),
				true
			);
			return InteractionResult.SUCCESS;
		}
		final BlockPos secondPos = getBlockPos(stack, SECOND_POS_TAG);
		if (secondPos == null) {
			final int area = Math.abs((pos.getX() - firstPos.getX()) * (pos.getY() - firstPos.getY()) * (pos.getZ() - firstPos.getZ()));
			if (area > MAX_BLOCK_LIMIT) {
				player.displayClientMessage(
					Component.literal(String.format("Area too large, trying %d, max %d", area, MAX_BLOCK_LIMIT)),
					true
				);
				return InteractionResult.FAIL;
			}
			setBlockPos(stack, SECOND_POS_TAG, pos);
			player.displayClientMessage(
				Component.literal(String.format("Second Pos Set - %d %d %d", pos.getX(), pos.getY(), pos.getZ())),
				true
			);
			return InteractionResult.SUCCESS;
		}
		clearBlockPos(stack);
		player.displayClientMessage(
			Component.literal("Area saved"),
			true
		);
		final PrintableSchematic schematic = PrintableSchematic.fromLevel(level, new AABBi(
			Math.min(firstPos.getX(), secondPos.getX()),
			Math.min(firstPos.getY(), secondPos.getY()),
			Math.min(firstPos.getZ(), secondPos.getZ()),
			Math.max(firstPos.getX(), secondPos.getX()),
			Math.max(firstPos.getY(), secondPos.getY()),
			Math.max(firstPos.getZ(), secondPos.getZ())
		));
		SchematicManager.get().putSchematic(schematic);
		stack.getOrCreateTag().putString(BLUEPRINT_TAG, schematic.getFingerprint());
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Level level, final List<Component> texts, final TooltipFlag flags) {
		super.appendHoverText(stack, level, texts, flags);
		final String blueprint = getBlueprint(stack);
		if (blueprint != null) {
			texts.add(Component.literal("Blueprint: ").append(blueprint.substring(0, 16)));
		}
	}

	private static BlockPos getBlockPos(final ItemStack stack, final String name) {
		final CompoundTag tag = stack.getTagElement(name);
		if (tag == null) {
			return null;
		}
		return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
	}

	private static void setBlockPos(final ItemStack stack, final String name, final BlockPos pos) {
		final CompoundTag tag = stack.getOrCreateTagElement(name);
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
	}

	private static void clearBlockPos(final ItemStack stack) {
		stack.removeTagKey(SECOND_POS_TAG);
		stack.removeTagKey(FIRST_POS_TAG);
	}

	public static String getBlueprint(final ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof QuantumFilmItem)) {
			return null;
		}
		final CompoundTag tag = stack.getTag();
		if (tag == null) {
			return null;
		}
		final String fingerprint = tag.getString(BLUEPRINT_TAG);
		return fingerprint.isEmpty() ? null : fingerprint;
	}
}
