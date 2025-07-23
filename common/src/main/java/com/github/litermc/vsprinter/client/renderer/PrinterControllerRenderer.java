package com.github.litermc.vsprinter.client.renderer;

import com.github.litermc.vsprinter.block.PrinterControllerBlock;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;
import com.github.litermc.vsprinter.block.PrinterFrameBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class PrinterControllerRenderer implements BlockEntityRenderer<PrinterControllerBlockEntity> {
	private final BlockEntityRendererProvider.Context context;

	public PrinterControllerRenderer(final BlockEntityRendererProvider.Context context) {
		this.context = context;
	}

	@Override
	public boolean shouldRender(final PrinterControllerBlockEntity be, final Vec3 pos) {
		return true;
	}

	@Override
	public void render(
		final PrinterControllerBlockEntity be,
		final float partialTick,
		final PoseStack poseStack, final MultiBufferSource bufferSource,
		final int packedLight, final int packedOverlay
	) {
		final Direction frameDirection = be.getBlockState().getValue(PrinterControllerBlock.FRAME).asDirection();
		if (frameDirection == null) {
			return;
		}
		final int frameLevel = be.getFrameLevel();
		if (frameLevel < 0) {
			return;
		}
		final double frameSize = switch (frameLevel) {
			case 0 -> 1.0 / 16;
			case 1 -> 3.0 / 16;
			case 2 -> 7.0 / 16;
			default -> 1;
		};
		final Level level = be.getLevel();
		final BlockPos blockPos = be.getBlockPos();

		final VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.cutoutMipped());

		final Vector3f offset = new Vector3f();
		final Vector4f color = new Vector4f(1f, 1f, 1f, 1f);

		PrinterFrameBlock.streamSelfAndConnected(level, blockPos.relative(frameDirection)).forEach((pos) -> {
			offset.set(pos.getX() - blockPos.getX(), pos.getY() - blockPos.getY(), pos.getZ() - blockPos.getZ());
		});
	}
}
