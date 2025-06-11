package com.github.litermc.vsprinter.api;

import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;

import org.valkyrienskies.core.api.ships.ServerShip;

public interface PrintPlugin {
	void onShipFinish(PrinterControllerBlockEntity controller, ServerShip ship);
}
