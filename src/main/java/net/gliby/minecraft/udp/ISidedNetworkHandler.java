package net.gliby.minecraft.udp;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public interface ISidedNetworkHandler {

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player) throws IOException;

	public void disconnect(EntityPlayer player);

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			IConnectionInformation connectionInformation) throws IOException;

	Side getSide();

	Logger getLogger();
	void sendUDP(GameProfile player, Object object);
}
