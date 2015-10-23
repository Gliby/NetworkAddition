package net.gliby.minecraft.udp.client;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.IConnectionInformation;
import net.gliby.minecraft.udp.SharedNetwork;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientNetworkHandler extends SharedNetwork {

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void clientToServerDestroyed(ClientDisconnectionFromServerEvent clientConnectionEvent) {
		EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
		disconnect(player);
	}

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

	private boolean authenticated;

	public void setAuthenticated(boolean b) {
		this.authenticated = b;
	}

	@Override
	public void sendUDP(GameProfile player, Object object) {
		
		//TODO Make this work.
		try {
			throw new Exception("Wrong side!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
