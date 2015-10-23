package net.gliby.minecraft.udp.packethandlers;

import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.SharedNetwork;
import net.minecraftforge.fml.relauncher.Side;

public interface IPacketHandler {

	public void handle(SharedNetwork networkHandler, IPlayerConnection playerConnection, Object object);

	public Side getSide();
}
