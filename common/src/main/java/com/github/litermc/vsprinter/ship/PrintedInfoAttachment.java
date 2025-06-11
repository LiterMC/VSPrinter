package com.github.litermc.vsprinter.ship;

import com.github.litermc.vsprinter.platform.PlatformHelper;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class PrintedInfoAttachment {
	private Long parentShip = null;
	private boolean preventBlockDrops = false;
	private boolean preventBlockPlacement = false;

	public PrintedInfoAttachment() {}

	public PrintedInfoAttachment(final Long parentShip, final boolean preventBlockDrops, final boolean preventBlockPlacement) {
		this.parentShip = parentShip;
		this.preventBlockDrops = preventBlockDrops;
		this.preventBlockPlacement = preventBlockPlacement;
	}

	public ServerShip getParentShip() {
		if (this.parentShip == null) {
			return null;
		}
		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		return shipWorld.getAllShips().getById(this.parentShip);
	}

	public boolean getPreventBlockDrops() {
		return this.preventBlockDrops;
	}

	public boolean getPreventBlockPlacement() {
		return this.preventBlockPlacement;
	}
}
