package net.gliby.minecraft.udp.client;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.google.common.collect.BiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.IConnectionInformation;
import net.gliby.minecraft.udp.ServerNetworkHandler;
import net.gliby.minecraft.udp.ServerPlayerConnection;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ClientNetworkHandler extends ServerNetworkHandler {

	private IConnectionInformation connectionInformation;
	private Client client;

	// Temporary
	public String getHost(Minecraft minecraft) {
		ServerData serverData;
		String serverAddress;
		if ((serverData = Minecraft.getMinecraft().getCurrentServerData()) != null) {
			final ServerAddress server = ServerAddress.func_78860_a(serverData.serverIP);
			serverAddress = server.getIP();
		} else
			serverAddress = "localhost";
		return serverAddress;
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			final IConnectionInformation connectionInformation) throws IOException {
		setAuthenticated(false);
		this.connectionInformation = connectionInformation;
		this.client = new Client() {

		};
		client.start();
		init(this, client);
		client.addListener(new Listener() {
			@Override
			public void connected(Connection connection) {
				InnerAuth auth = new InnerAuth(connectionInformation.getKey());
				client.sendTCP(auth);
			}
		});

		System.out.println(
				"Address: " + FMLClientHandler.instance().getClientToServerNetworkManager().getRemoteAddress());
		client.connect(5000, getHost(Minecraft.getMinecraft()), connectionInformation.getTCP(),
				connectionInformation.getUDP());
	}

	@Override
	public void disconnect(EntityPlayer player) {
		setAuthenticated(false);
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

	@Override
	public BiMap<ServerPlayerConnection, GameProfile> getActiveConnections() {
		try {
			throw new Exception("Wrong side.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean authenticated;

	public void setAuthenticated(boolean b) {
		this.authenticated = b;
	}
}
