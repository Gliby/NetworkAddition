package net.gliby.minecraft.udp;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import net.minecraft.entity.player.EntityPlayer;

public class ClientNetworkHandler extends ServerNetworkHandler {

	// Temporary
	public String getHost() {
		return "localhost";
	}

	@Override
	public void connect(EntityPlayer player) throws IOException {
		System.out.println("Connected: " + player);
		final Client client = new Client();
		client.addListener(new Listener() {

			@Override
			public void connected(Connection connection) {
				client.sendTCP("Hey there!");
			}

		});
		client.start();
		client.connect(5000, getHost(), this.getTCPPort(), this.getUDPPort());
	}

	@Override
	public void disconnect(EntityPlayer player) {

	}

}
