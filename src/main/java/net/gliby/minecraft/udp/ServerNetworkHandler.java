package net.gliby.minecraft.udp;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.minecraft.entity.player.EntityPlayer;

public class ServerNetworkHandler implements ISidedNetworkHandler {

	// TODO Settings!
	public int getTCPPort() {
		return 25568;
	}
	
	public int getUDPPort() {
		return 25567;
	}

	@Override
	public void connect(EntityPlayer player) throws IOException {

	}

	@Override
	public void disconnect(EntityPlayer player) {

	}

	Server server;

	public final void start(AdditionalNetwork additionalNetwork) throws IOException {
		server = new Server() {
			@Override
			protected Connection newConnection() {
				// Provide our own custom Connection object.
				return new PlayerConnection();
			}
		};
		server.addListener(new Listener() {

			@Override
			public void received(Connection connection, Object object) {
				PlayerConnection playerConnection = (PlayerConnection) connection;
				System.out.println("Received: " + object.toString());
			}
		});
		server.bind(getTCPPort(), getUDPPort());
		server.start();
		
	}

	public void register(EndPoint point) {
	}

}
