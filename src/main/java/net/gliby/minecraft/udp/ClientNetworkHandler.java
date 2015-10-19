package net.gliby.minecraft.udp;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class ClientNetworkHandler extends ServerNetworkHandler {

	// Temporary
	public String getHost() {
		return "localhost";
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player) throws IOException {
		/*
		 * final Client client = new Client(); client.addListener(new Listener()
		 * {
		 * 
		 * @Override public void connected(Connection connection) { }
		 * 
		 * }); client.start(); client.connect(5000, getHost(),
		 * this.getTCPPort(), this.getUDPPort());
		 */
	}

	@Override
	public void disconnect(EntityPlayer player) {

	}

}
