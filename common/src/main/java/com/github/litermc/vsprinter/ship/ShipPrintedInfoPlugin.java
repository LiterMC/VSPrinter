package com.github.litermc.vsprinter.ship;

import com.github.litermc.vsprinter.api.PrintPlugin;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ShipPrintedInfoPlugin implements PrintPlugin {
	public static final ShipPrintedInfoPlugin INSTANCE = new ShipPrintedInfoPlugin();

	@Override
	public void onShipFinish(final PrinterControllerBlockEntity controller, final ServerShip ship) {
		final Ship parentShip = VSGameUtilsKt.getShipManagingPos(controller.getLevel(), controller.getBlockPos());
		final double scale = ship.getTransform().getShipToWorldScaling().lengthSquared();
		ship.saveAttachment(
			PrintedInfoAttachment.class,
			new PrintedInfoAttachment(
				parentShip == null ? null : parentShip.getId(),
				scale < 3,
				true
			)
		);
	}
}
