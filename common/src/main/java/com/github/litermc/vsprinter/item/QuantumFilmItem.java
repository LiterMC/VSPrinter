package com.github.litermc.vsprinter.item;

import com.github.litermc.vsprinter.api.PrintableSchematic;
import com.github.litermc.vsprinter.api.SchematicManager;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;

import net.minecraft.ChatFormatting;
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
	public static final String BLUEPRINT_TAG = "Blueprint";
	private static final String DIMENSION_TAG = "Dimension";

	public QuantumFilmItem(final Item.Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(final UseOnContext ctx) {
		final Player player = ctx.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		final Level level = ctx.getLevel();
		final ItemStack stack = ctx.getItemInHand();
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}

		BlockPos assigningPos = ctx.getClickedPos();

		if (player.isShiftKeyDown()) {
			if (level.getBlockEntity(assigningPos) instanceof PrinterControllerBlockEntity printer) {
				if (getBlueprint(stack) != null && printer.putBlueprintItem(stack.copy())) {
					stack.shrink(1);
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.FAIL;
			}
		}

		if (ctx.getClickedFace().getStepY() != 0) {
			assigningPos = assigningPos.relative(ctx.getClickedFace());
		}

		return onSetPos(stack, level, player, assigningPos);
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
		final PrintableSchematic schematic = PrintableSchematic.fromLevel(level, new AABBi(
			Math.min(firstPos.getX(), secondPos.getX()),
			Math.min(firstPos.getY(), secondPos.getY()),
			Math.min(firstPos.getZ(), secondPos.getZ()),
			Math.max(firstPos.getX(), secondPos.getX()),
			Math.max(firstPos.getY(), secondPos.getY()),
			Math.max(firstPos.getZ(), secondPos.getZ())
		));
		if (schematic == null) {
			stack.removeTagKey(BLUEPRINT_TAG);
			player.displayClientMessage(
				Component.literal("Blueprint cleared"),
				true
			);
			return InteractionResult.SUCCESS;
		}
		SchematicManager.get().putSchematic(schematic);
		stack.getOrCreateTag().putString(BLUEPRINT_TAG, schematic.getFingerprint());
		final CompoundTag dimTag = stack.getOrCreateTagElement(DIMENSION_TAG);
		dimTag.putInt("x", schematic.getDimension().getX());
		dimTag.putInt("y", schematic.getDimension().getY());
		dimTag.putInt("z", schematic.getDimension().getZ());
		player.displayClientMessage(
			Component.literal("Area saved"),
			true
		);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Level level, final List<Component> texts, final TooltipFlag flags) {
		super.appendHoverText(stack, level, texts, flags);
		final String blueprint = getBlueprint(stack);
		if (blueprint != null) {
			texts.add(
				Component.literal("Blueprint: ")
					.withStyle(ChatFormatting.GRAY)
					.append(Component.literal(blueprint.substring(0, 16)).withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE))
			);
			final CompoundTag dimension = stack.getTagElement(DIMENSION_TAG);
			if (dimension != null) {
				final int xSize = dimension.getInt("x");
				final int ySize = dimension.getInt("y");
				final int zSize = dimension.getInt("z");
				texts.add(
					Component.literal("Dimension: ")
						.withStyle(ChatFormatting.GRAY)
						.append(String.format("%d*%d*%d", xSize, ySize, zSize))
				);
			}
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
