package net.gliby.minecraft.udp;

import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

//TODO Add security mesaures, kick from server with client has authenticated,
//TODO Don't allow more than 1 auth per player(each time wrong, multiply deny time by 2, clamp at 5 minutes.)

public class ServerNetworkHandler implements ISidedNetworkHandler {

	private BiMap<PlayerConnection, GameProfile> activeConnections;
	private BiMap<GameProfile, String> awaitingAuthentication;
	private HashMap<GameProfile, String> authenticated;

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
			String key = secureId.nextId(130, 32);
			awaitingAuthentication.put(player.getGameProfile(), key);
			networkDispatcher.sendTo(new PacketAuthentication(key, getTCPPort(), getUDPPort()),
					(EntityPlayerMP) player);
		}
	}

	@Override
	public void disconnect(EntityPlayer player) {
		authenticated.remove(player.getGameProfile());
		awaitingAuthentication.remove(player.getGameProfile());
		PlayerConnection connection = activeConnections.inverse().get(player.getGameProfile());
		if (connection != null) {
			connection.close();
			activeConnections.remove(connection);
		}
	}

	SessionIdentifierGenerator secureId;
	Server server;

	public final void stop(AdditionalNetwork additionalNetwork) {
		server.stop();
		try {
			server.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void start(AdditionalNetwork additionalNetwork) throws IOException {
		secureId = new SessionIdentifierGenerator();
		authenticated = new HashMap<GameProfile, String>();
		awaitingAuthentication = HashBiMap.create();
		activeConnections = HashBiMap.create();
		server = new Server() {
			@Override
			protected Connection newConnection() {
				// Provide our own custom Connection object.
				return new PlayerConnection();
			}
		};
		register(server);

		server.addListener(new Listener() {

			@Override
			public void received(Connection connection, Object object) {
				PlayerConnection playerConnection = (PlayerConnection) connection;
				System.out.println("listen: " + object);
				if (object instanceof InnerAuth && !playerConnection.isValid()) {
					InnerAuth auth = (InnerAuth) object;
					System.out.println("auth begin: " + auth.key + ", size: " + auth.key.length());
					BiMap<String, GameProfile> reversed = awaitingAuthentication.inverse();
					GameProfile gameProfile;
					if (auth.key.length() == 26 && (gameProfile = reversed.get(auth.key)) != null) {
						System.out.println("Player validated");
						playerConnection.validate(reversed.get(auth.key));
						activeConnections.put(playerConnection, gameProfile);
					} else {
						connection.close();
						System.out.println("kick and block");
					}
				} else if (playerConnection.isValid()) {
					if (object instanceof IMessage) {
						IMessage mcPacket = (IMessage) object;
						// TODO Add handler
					}
					// TODO Add kryonet specific.
				}
			}

			@Override
			public void disconnected(Connection connection) {
				PlayerConnection playerConnection = (PlayerConnection) connection;
				activeConnections.remove(playerConnection.getGameProfile());
			}
		});
		server.bind(getTCPPort(), getUDPPort());
		server.start();
		additionalNetwork.getLogger().info("Started: " + server);
	}

	public void register(EndPoint point) {
		point.getKryo().register(InnerAuth.class);
	}

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			IConnectionInformation connectionInformation) throws IOException {
	}

}
