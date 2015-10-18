package net.gliby.minecraft.udp;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

public interface ISidedNetworkHandler {

	public void connect(EntityPlayer player) throws IOException;

	public void disconnect(EntityPlayer player);

}
