package net.gliby.minecraft.udp.packethandlers;

import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.ISidedNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;

public interface IPacketHandler {

	public void handle(ISidedNetworkHandler networkHandler, IPlayerConnection playerConnection, Object object);

	public Side getSide();
}
