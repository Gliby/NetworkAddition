package net.gliby.minecraft.udp;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public interface ISidedNetworkHandler {

	public void connect(SimpleNetworkWrapper networkDispatcher,  EntityPlayer player) throws IOException;

	public void disconnect(EntityPlayer player);


}
