package net.gliby.minecraft.udp;

import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class ServerNetworkHandler implements ISidedNetworkHandler {

	private HashMap<GameProfile, String> awaitingAuthentication;
	private BiMap<GameProfile, String> authenticated;

	// TODO Settings!
	public int getTCPPort() {
		return 25568;
	}

	public int getUDPPort() {
		return 25567;
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player) throws IOException {
		if (!authenticated.containsKey(player.getGameProfile())) {
			String key = secureId.nextId();
			awaitingAuthentication.put(player.getGameProfile(), key);
			networkDispatcher.sendTo(new PacketAuthentication(key), (EntityPlayerMP) player);
		}
	}

	@Override
	public void disconnect(EntityPlayer player) {
		authenticated.remove(player.getGameProfile());
		awaitingAuthentication.remove(player.getGameProfile());
	}

	SessionIdentifierGenerator secureId;
	Server server;

	public final void start(AdditionalNetwork additionalNetwork) throws IOException {
		secureId = new SessionIdentifierGenerator();
		awaitingAuthentication = new HashMap<GameProfile, String>();
		authenticated = HashBiMap.create();
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
				if (object instanceof InnerAuth && !playerConnection.isValid()) {
					InnerAuth auth = (InnerAuth) object;
					BiMap<String, GameProfile> reversed = authenticated.inverse();
					if (auth.key.length() == 32 && reversed.containsKey(auth.key)) {
						playerConnection.validate(reversed.get(auth.key));
					}
				} else if (playerConnection.isValid()) {
					if (object instanceof IMessage) {
						IMessage mcPacket = (IMessage) object;
						//TODO Add handler
					}
					//TODO Add kryonet specific.
				}
			}
		});
		server.bind(getTCPPort(), getUDPPort());
		server.start();
		additionalNetwork.getLogger().info("Started: " + server);
	}

	public void register(EndPoint point) {
	}

}
