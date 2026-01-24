package com.github.litermc.vsprinter.attachment;

import com.github.litermc.vsprinter.platform.PlatformHelper;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class PrintedInfoAttachment {
	private Long parentShip = null;
	private boolean preventBlockDrops = false;
	private boolean preventBlockPlacement = false;

	public PrintedInfoAttachment() {}

	private PrintedInfoAttachment(final Long parentShip, final boolean preventBlockDrops, final boolean preventBlockPlacement) {
		this.parentShip = parentShip;
		this.preventBlockDrops = preventBlockDrops;
		this.preventBlockPlacement = preventBlockPlacement;
	}

	public static PrintedInfoAttachment createOn(
		final ServerShip ship,
		final Long parentShip,
		final boolean preventBlockDrops,
		final boolean preventBlockPlacement
	) {
		final PrintedInfoAttachment attachment = new PrintedInfoAttachment(parentShip, preventBlockDrops, preventBlockPlacement);
		if (ship instanceof final LoadedServerShip loadedShip) {
			loadedShip.setAttachment(attachment);
		} else if (ship instanceof final ShipData shipData) {
			final var attachmentHolder = shipData.getAttachmentHolder();
			attachmentHolder.setAttachment(attachment);
		} else {
			throw new IllegalArgumentException("ship is neither LoadedServerShip nor ShipData");
		}
		return attachment;
	}

	public static PrintedInfoAttachment get(final ServerShip ship) {
		if (ship instanceof final LoadedServerShip loadedShip) {
			return loadedShip.getAttachment(PrintedInfoAttachment.class);
		}
		if (ship instanceof final ShipData shipData) {
			final var attachmentHolder = shipData.getAttachmentHolder();
			return attachmentHolder.getAttachment(PrintedInfoAttachment.class);
		}
		throw new IllegalArgumentException("ship is neither LoadedServerShip nor ShipData");
	}

	public ServerShip getParentShip() {
		if (this.parentShip == null) {
			return null;
		}
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		return shipWorld.getAllShips().getById(this.parentShip);
	}

	public boolean getPreventBlockDrops() {
		return this.preventBlockDrops;
	}

	public boolean getPreventBlockPlacement() {
		return this.preventBlockPlacement;
	}
}
