package net.gliby.minecraft.udp;

import net.minecraft.entity.player.EntityPlayer;

public interface SidedNetworkHandler {

	public void connect(EntityPlayer player);

	public void disconnect(EntityPlayer player);

}
