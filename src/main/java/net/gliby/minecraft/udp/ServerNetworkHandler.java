package net.gliby.minecraft.udp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.packethandlers.IPacketHandler;
import net.gliby.minecraft.udp.packets.IAdditionalHandler;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.security.Authenticator;
import net.gliby.minecraft.udp.security.Authenticator.IValidation;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

//TODO Add security measures.
//TODO Don't allow more than 1 auth per player(each time wrong, multiply deny time by 1.2, clamp at 5 minutes.)

public class ServerNetworkHandler implements ISidedNetworkHandler {

	private HashMap<Class, IPacketHandler> externalPacketHandlers = new HashMap<Class, IPacketHandler>();

	public HashMap<Class, IPacketHandler> getExternalPacketHandlers() {
		return externalPacketHandlers;
	}

	protected void init(final ISidedNetworkHandler networkHandler, EndPoint point) {
		Log.setLogger(new AnotherLogger(networkHandler.getLogger()));
		point.getKryo().register(InnerAuth.class);
		packetHandlers = new HashMap<Class, IPacketHandler>();
		for (Entry<Class, IPacketHandler> entry : externalPacketHandlers.entrySet()) {
			point.getKryo().register(entry.getKey());
			for (Field field : entry.getKey().getDeclaredFields()) {
				point.getKryo().register(field.getClass());
			}
			if (entry.getValue().getSide() == getSide())
				packetHandlers.put(entry.getKey(), entry.getValue());
		}

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
				if ((handler = packetHandlers.get(obj.getClass())) != null) {
					if (handler.getSide() == getSide())
						handler.handle(networkHandler, connection, obj);
					else {
						networkHandler.getLogger()
								.error("Wrong side for packet handler: " + handler + "(" + obj + ").");
					}
				} else {
					networkHandler.getLogger().debug("No packet handler found for: " + obj);
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
	private HashMap<Class, IPacketHandler> packetHandlers;

	/**
	 * All active connections.
	 */
	private BiMap<ServerPlayerConnection, GameProfile> activeConnections;

	private Authenticator getAuthenticator() {
		return authenticator;
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
		EntityPlayerMP mp = (EntityPlayerMP) player;
		// HijackedNetPlayerHandler handler = (HijackedNetPlayerHandler)
		// CloneHelper.cloneObject(mp.playerNetServerHandler,
		// HijackedNetPlayerHandler.class);
		mp.playerNetServerHandler = new HijackedNetPlayerHandler(MinecraftServer.getServer(),
				mp.playerNetServerHandler.getNetworkManager(), mp);
		((HijackedNetPlayerHandler) mp.playerNetServerHandler).bus().register(new PacketInterceptEvent(this));

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
		init(this, server);
		server.addListener(new Listener() {

			@Override
			public void connected(Connection connection) {
				// TODO Accept/deny connection;
			}

			@Override
			public void received(Connection connection, Object object) {
				ServerPlayerConnection playerConnection = (ServerPlayerConnection) connection;
				if (object instanceof InnerAuth && !playerConnection.isValid()) {
					InnerAuth auth = (InnerAuth) object;
					IValidation validation;
					if ((validation = authenticator.getValidation(auth.key)) != null) {
						additionalNetwork.getLogger().info("Connection[" + connection.getID() + "] will be known as "
								+ validation.getOwner().getName() + ".");
						playerConnection.validate(validation);
						activeConnections.put(playerConnection, validation.getOwner());
						server.sendToTCP(connection.getID(), new PacketAuthentication());
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

	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player,
			IConnectionInformation connectionInformation) throws IOException {
	}

	@Override
	public Side getSide() {
		return Side.SERVER;
	}

	@Override
	public Logger getLogger() {
		return AdditionalNetwork.getInstance().getLogger();
	}

	private IPacketHandler serverPacketHandler = new IPacketHandler() {

		@Override
		public void handle(ISidedNetworkHandler networkHandler, IPlayerConnection playerConnection, Object object) {
			if (object instanceof IAdditionalHandler<?>) {
				((IAdditionalHandler) object).handle(networkHandler, playerConnection, object);
			} else {
				networkHandler.getLogger().fatal(IAdditionalHandler.class + " is not present.");
			}
		}

		@Override
		public Side getSide() {
			return Side.SERVER;
		}
	};

	private IPacketHandler clientPacketHandler = new IPacketHandler() {

		@Override
		public void handle(ISidedNetworkHandler networkHandler, IPlayerConnection playerConnection, Object object) {
			if (object instanceof IAdditionalHandler<?>) {
				((IAdditionalHandler) object).handle(networkHandler, playerConnection, object);
			} else {
				networkHandler.getLogger().fatal(IAdditionalHandler.class + " is not present.");
			}
		}

		@Override
		public Side getSide() {
			return Side.CLIENT;
		}
	};

	public IPacketHandler getServerDefaultPacketHandler() {
		return serverPacketHandler;
	}

	public IPacketHandler getClientDefaultPacketHandler() {
		return clientPacketHandler;
	}

	@Override
	public void sendUDP(EntityPlayer player, Object object) {
		server.sendToUDP(getActiveConnections().inverse().get(player.getGameProfile()).getID(), object);
	}

}
