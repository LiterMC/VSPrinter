package com.github.litermc.vsprinter.client.renderer;

import com.github.litermc.vsprinter.Constants;
import com.github.litermc.vsprinter.api.PrintStatus;
import com.github.litermc.vsprinter.api.resource.ModelTextures;
import com.github.litermc.vsprinter.api.resource.TextureLocation;
import com.github.litermc.vsprinter.block.PrinterControllerBlock;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;
import com.github.litermc.vsprinter.block.PrinterFrameBlock;
import com.github.litermc.vsprinter.client.RenderUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.primitives.AABBi;

import java.util.EnumSet;
import java.util.List;

public class PrinterControllerRenderer implements BlockEntityRenderer<PrinterControllerBlockEntity> {
	private static final Quaternionf QUAT_ZERO = new Quaternionf();

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
		final Level level = be.getLevel();
		final BlockPos blockPos = be.getBlockPos();
		final FrontAndTop frontAndTop = be.getBlockState().getValue(PrinterControllerBlock.ORIENTATION);
		final PrintStatus status = be.getStatus();
		final int displayLight = 0xf000f0;

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.mulPose(frontAndTop.front().getRotation());
		poseStack.mulPose(frontAndTop.top().getRotation());
		poseStack.translate(0, 0, -0.501f);

		poseStack.pushPose();
		poseStack.translate(5f / 16, 3f / 16, 0);

		final String displayText = status.getDisplayText().getString();
		final float displayTextScale = 1f / 64;
		final float displayTextWidth = this.context.getFont().width(displayText) * displayTextScale;

		poseStack.pushPose();
		poseStack.translate(0, 1f / 16, 0);
		poseStack.scale(-displayTextScale, -displayTextScale, displayTextScale);
		this.context.getFont().drawInBatch(
			displayText,
			0, 0,
			0xffffff,
			false,
			poseStack.last().pose(),
			bufferSource,
			Font.DisplayMode.POLYGON_OFFSET,
			0x000000,
			displayLight
		);
		poseStack.popPose();

		switch (status) {
			case REQUIRE_MATERIAL -> {
				final ItemStack requiredItem = be.getRequiredItem();
				if (requiredItem != null) {
					poseStack.pushPose();
					poseStack.translate(-displayTextWidth - 1.5f / 16, 0, 0);
					poseStack.scale(0.25f, 0.25f, 0.25f);
					this.context.getItemRenderer().renderStatic(
						requiredItem,
						ItemDisplayContext.FIXED,
						displayLight,
						OverlayTexture.NO_OVERLAY,
						poseStack,
						bufferSource,
						level,
						0
					);
					poseStack.popPose();
				}
			}
		}

		poseStack.popPose();
		poseStack.popPose();

		final Direction frameDirection = be.getBlockState().getValue(PrinterControllerBlock.FRAME).asDirection();
		if (frameDirection == null) {
			return;
		}

		final Vector3f baseOffset = new Vector3f();
		final Vector3f offset = new Vector3f();
		final Vector4f frameColor = new Vector4f(0.31f, 0.31f, 0.33f, 1f);

		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();

		final List<BlockPos> frames = PrinterFrameBlock.streamSelfAndConnected(level, blockPos.relative(frameDirection)).toList();
		final AABBi box = PrinterFrameBlock.getFrameBox(frames);

		if (box == null) {
			return;
		}

		final float frameSize = switch (be.calcFrameLevel(box)) {
			case 0 -> 1f / 16;
			case 1 -> 3f / 16;
			case 2 -> 7f / 16;
			default -> 1;
		};

		final VertexConsumer frameBuffer = bufferSource.getBuffer(RenderType.textBackground());

		frames.forEach((pos) -> {
			final float center = 0.5f - frameSize / 2;

			final BlockState frameState = level.getBlockState(pos);
			final EnumSet<Direction> faces = PrinterFrameBlock.getFacesFromBox(box, pos);
			lightMap.setAll(LevelRenderer.getLightColor(level, pos));
			baseOffset.set(pos.getX() - blockPos.getX(), pos.getY() - blockPos.getY(), pos.getZ() - blockPos.getZ());

			if (faces.contains(Direction.DOWN)) {
				if (faces.contains(Direction.WEST)) {
					baseOffset.add(-center, -center, 0, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, frameSize, 1));
				}
				if (faces.contains(Direction.EAST)) {
					baseOffset.add(center, -center, 0, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, frameSize, 1));
				}
				if (faces.contains(Direction.NORTH)) {
					baseOffset.add(0, -center, -center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(1, frameSize, frameSize));
				}
				if (faces.contains(Direction.SOUTH)) {
					baseOffset.add(0, -center, center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(1, frameSize, frameSize));
				}
			}
			if (faces.contains(Direction.UP)) {
				if (faces.contains(Direction.WEST)) {
					baseOffset.add(-center, center, 0, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, frameSize, 1));
				}
				if (faces.contains(Direction.EAST)) {
					baseOffset.add(center, center, 0, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, frameSize, 1));
				}
				if (faces.contains(Direction.NORTH)) {
					baseOffset.add(0, center, -center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(1, frameSize, frameSize));
				}
				if (faces.contains(Direction.SOUTH)) {
					baseOffset.add(0, center, center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(1, frameSize, frameSize));
				}
			}
			if (faces.contains(Direction.NORTH)) {
				if (faces.contains(Direction.WEST)) {
					baseOffset.add(-center, 0, -center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, 1, frameSize));
				}
				if (faces.contains(Direction.EAST)) {
					baseOffset.add(center, 0, -center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, 1, frameSize));
				}
			}
			if (faces.contains(Direction.SOUTH)) {
				if (faces.contains(Direction.WEST)) {
					baseOffset.add(-center, 0, center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, 1, frameSize));
				}
				if (faces.contains(Direction.EAST)) {
					baseOffset.add(center, 0, center, offset);
					RenderUtil.drawBox(poseStack, frameBuffer, lightMap, frameColor, offset, QUAT_ZERO, new Vector3f(frameSize, 1, frameSize));
				}
			}
		});
	}
}
