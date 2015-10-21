package net.gliby.minecraft.udp.client;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import net.gliby.minecraft.udp.IConnectionInformation;
import net.gliby.minecraft.udp.ServerNetworkHandler;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ClientNetworkHandler extends ServerNetworkHandler {

	private IConnectionInformation connectionInformation;
	private Client client;

	// Temporary
	public String getHost() {
		return "localhost";
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			final IConnectionInformation connectionInformation) throws IOException {
		this.connectionInformation = connectionInformation;
		this.client = new Client() {
			
		};
		client.start();
		initializeNetwork(this, client);
		client.addListener(new Listener() {
			@Override
			public void connected(Connection connection) {
				InnerAuth auth = new InnerAuth(connectionInformation.getKey());
				client.sendTCP(auth);
			}
		});
		client.connect(5000, getHost(), connectionInformation.getTCP(), connectionInformation.getUDP());
	}

	@Override
	public void disconnect(EntityPlayer player) {
		client.close();
		client.stop();
		try {
			client.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Side getSide() {
		return Side.CLIENT;
	}

}