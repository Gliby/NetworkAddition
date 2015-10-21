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
import net.minecraft.network.INetHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

//TODO Add security measures.
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
		removeConnection(player.getGameProfile(), activeConnections.inverse().get(player.getGameProfile()));
	}

	private void removeConnection(GameProfile profile, Connection connection) {
		authenticated.remove(profile);
		awaitingAuthentication.remove(profile);
		if (connection != null) {
			activeConnections.remove(connection);
			connection.close();
		}
	}

	private SessionIdentifierGenerator secureId;
	private Server server;

	public final void stop(AdditionalNetwork additionalNetwork) {
		server.stop();
		try {
			server.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void start(final AdditionalNetwork additionalNetwork) throws IOException {
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
		registerObjects(server);

		server.addListener(new Listener() {

			@Override
			public void received(Connection connection, Object object) {
				PlayerConnection playerConnection = (PlayerConnection) connection;
				if (object instanceof InnerAuth && !playerConnection.isValid()) {
					InnerAuth auth = (InnerAuth) object;
					System.out.println("auth begin: " + auth.key + ", size: " + auth.key.length());
					BiMap<String, GameProfile> reversed = awaitingAuthentication.inverse();
					GameProfile gameProfile;
					if (auth.key.length() == 26 && (gameProfile = reversed.get(auth.key)) != null) {
						System.out.println("Player validated");
						additionalNetwork.getLogger().info(
								"Connection[" + connection.getID() + "] will be known as " + gameProfile.getName() + ".");
						playerConnection.validate(reversed.get(auth.key));
						activeConnections.put(playerConnection, gameProfile);
					} else {
						connection.close();
						//TODO Actually implement...
						System.out.println("kick and block");
					}
				} else if (playerConnection.isValid()) {
					if (object instanceof IMessage) {
						handleMessagePacket(additionalNetwork, (IMessage) object, playerConnection);
					} else {
						handleKryo(object, playerConnection);
					}
				}
			}

			@Override
			public void disconnected(Connection connection) {
				PlayerConnection playerConnection = (PlayerConnection) connection;
				removeConnection(playerConnection.getGameProfile(), playerConnection);
			}
		});
		server.bind(getTCPPort(), getUDPPort());
		server.start();
		additionalNetwork.getLogger().info("Started: " + server);
	}

	protected void handleKryo(Object object, PlayerConnection playerConnection) {

	}

	protected void handleMessagePacket(AdditionalNetwork additonalNetwork, IMessage object,
			PlayerConnection playerConnection) {
		if (object instanceof IAdditionalHandler<?>) {
			((IAdditionalHandler) object).handle(object, FMLCommonHandler.instance().getSide());
		}
	}

	public void registerObjects(EndPoint point) {
		point.getKryo().register(InnerAuth.class);
	}

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			IConnectionInformation connectionInformation) throws IOException {
	}

}
