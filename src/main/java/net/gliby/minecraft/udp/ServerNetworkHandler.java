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

import net.gliby.minecraft.udp.packethandlers.IPacketHandler;
import net.gliby.minecraft.udp.packets.IAdditionalHandler;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.security.Authenticator;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.gliby.minecraft.udp.security.Authenticator.IValidation;
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
//TODO Don't allow more than 1 auth per player(each time wrong, multiply deny time by 1.2, clamp at 5 minutes.)

public class ServerNetworkHandler implements ISidedNetworkHandler {

	protected void initializeNetwork(final ISidedNetworkHandler networkHandler, EndPoint point) {
		point.getKryo().register(InnerAuth.class);

		packetHandlers = new HashMap<Object, IPacketHandler>();
		point.addListener(new Listener() {

			@Override
			public void received(Connection c, Object obj) {
				IPlayerConnection connection = null;
				if (c instanceof IPlayerConnection) {
					connection = (IPlayerConnection) c;
					if (!connection.isValid()) {
						return;
					}
				}

				IPacketHandler handler;
				if ((handler = packetHandlers.get(obj)) != null) {
					handler.handle(networkHandler, connection, obj);
				} else {
					AdditionalNetwork.getInstance().getLogger().fatal("No packet handler found for: " + obj);
				}
			}
		});
	}

	/**
	 * Handles authentication.
	 */
	private Authenticator authenticator;
	/**
	 * Corresponding packet handlers to each object.
	 */
	private HashMap<Object, IPacketHandler> packetHandlers;

	/**
	 * All active connections.
	 */
	private BiMap<ServerPlayerConnection, GameProfile> activeConnections;

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public HashMap<Object, IPacketHandler> getPacketHandlers() {
		return packetHandlers;
	}

	public BiMap<ServerPlayerConnection, GameProfile> getActiveConnections() {
		return activeConnections;
	}

	// TODO Settings!
	private int getTCPPort() {
		return 25568;
	}

	private int getUDPPort() {
		return 25567;
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player) throws IOException {
		String key;
		if ((key = authenticator.getAuthenticationKey(player.getGameProfile())) != null)
			networkDispatcher.sendTo(new PacketAuthentication(key, getTCPPort(), getUDPPort()),
					(EntityPlayerMP) player);
	}

	@Override
	public void disconnect(EntityPlayer player) {
		removeConnection(player.getGameProfile(), activeConnections.inverse().get(player.getGameProfile()));
	}

	private void removeConnection(GameProfile profile, Connection connection) {
		authenticator.removeAuthentication(profile);
		if (connection != null) {
			activeConnections.remove(connection);
			connection.close();
		}
	}

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
		activeConnections = HashBiMap.create();
		authenticator = new Authenticator();
		server = new Server() {
			@Override
			protected Connection newConnection() {
				// Provide our own custom Connection object.
				return new ServerPlayerConnection();
			}
		};
		initializeNetwork(this, server);
		server.addListener(new Listener() {

			@Override
			public void connected(Connection connection) {
				// TODO Accept/deny connection;
			}

			@Override
			public void received(Connection connection, Object object) {
				ServerPlayerConnection playerConnection = (ServerPlayerConnection) connection;
				if (object instanceof InnerAuth && !playerConnection.isValid()) {
					System.out.println("Auth rec");
					InnerAuth auth = (InnerAuth) object;
					IValidation validation;
					System.out.println(auth.key);
					if ((validation = authenticator.getValidation(auth.key)) != null) {
						additionalNetwork.getLogger().info("Connection[" + connection.getID() + "] will be known as "
								+ validation.getOwner().getName() + ".");
						playerConnection.validate(validation);
						activeConnections.put(playerConnection, validation.getOwner());
					} else {
						// TODO Actually implement...
						connection.close();
						System.out.println("kick and block");
					}
				}
			}

			@Override
			public void disconnected(Connection connection) {
				ServerPlayerConnection playerConnection = (ServerPlayerConnection) connection;
				removeConnection(playerConnection.getGameProfile(), playerConnection);
			}
		});
		server.bind(getTCPPort(), getUDPPort());
		server.start();
		additionalNetwork.getLogger().info("Started: " + server);
	}

	protected void handleKryo(Object object, ServerPlayerConnection playerConnection) {

	}

	protected void handleMessagePacket(AdditionalNetwork additonalNetwork, IMessage object,
			ServerPlayerConnection playerConnection) {
		if (object instanceof IAdditionalHandler<?>) {
			((IAdditionalHandler) object).handle(object, FMLCommonHandler.instance().getSide());
		}
	}

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			IConnectionInformation connectionInformation) throws IOException {
	}

	@Override
	public Side getSide() {
		return Side.SERVER;
	}
}
