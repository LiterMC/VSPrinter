// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsprinter;

import com.github.litermc.vsprinter.block.PrinterControllerBlock;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;
import com.github.litermc.vsprinter.block.PrinterFrameBlock;
import com.github.litermc.vsprinter.platform.PlatformHelper;
import com.github.litermc.vsprinter.platform.RegistrationHelper;
import com.github.litermc.vsprinter.platform.RegistryEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public final class VSPRegistry {
	private VSPRegistry() {}

	public static final class Blocks {
		private static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK);

		public static final RegistryEntry<Block> PRINTER_CONTROLLER =
			REGISTRY.register("printer_controller", () -> new PrinterControllerBlock(BlockBehaviour.Properties.of().strength(3f)));

		private Blocks() {}
	}

	public static final class BlockEntities {
		private static final RegistrationHelper<BlockEntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK_ENTITY_TYPE);

		private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> ofBlock(final RegistryEntry<? extends Block> block, final BiFunction<BlockPos, BlockState, T> factory) {
			return REGISTRY.register(block.id().getPath(), () -> PlatformHelper.get().createBlockEntityType(factory, block.get()));
		}

		public static final RegistryEntry<BlockEntityType<PrinterControllerBlockEntity>> PRINTER_CONTROLLER =
			ofBlock(Blocks.PRINTER_CONTROLLER, PrinterControllerBlockEntity::new);

		private BlockEntities() {}
	}

	public static final class Items {
		private static final RegistrationHelper<Item> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.ITEM);

		private static Item.Properties properties() {
			return new Item.Properties();
		}

		private static <B extends Block, I extends Item> RegistryEntry<I> ofBlock(RegistryEntry<B> block, BiFunction<B, Item.Properties, I> supplier) {
			return REGISTRY.register(block.id().getPath(), () -> supplier.apply(block.get(), properties()));
		}

		public static final RegistryEntry<BlockItem> PRINTER_CONTROLLER = ofBlock(Blocks.PRINTER_CONTROLLER, BlockItem::new);

		private Items() {}
	}
}
