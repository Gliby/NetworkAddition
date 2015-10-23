package net.gliby.minecraft.udp.server;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.AdditionalNetwork;
import net.gliby.minecraft.udp.IConnectionInformation;
import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.SharedNetwork;
import net.gliby.minecraft.udp.packethandlers.IPacketHandler;
import net.gliby.minecraft.udp.packets.IAdditionalHandler;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.security.Authenticator;
import net.gliby.minecraft.udp.security.Authenticator.IValidation;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerDisconnectionFromClientEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

//TODO Add security measures.
//TODO Don't allow more than 1 auth per player(each time wrong, multiply deny time by 1.2, clamp at 5 minutes.)

public class ServerNetworkHandler extends SharedNetwork {

	private HashMap<Class, IPacketHandler> externalPacketHandlers = new HashMap<Class, IPacketHandler>();

	public HashMap<Class, IPacketHandler> getExternalPacketHandlers() {
		return externalPacketHandlers;
	}

	@SubscribeEvent
	public void serverToClientEstablished(final PlayerLoggedInEvent serverConnectionEvent) {
		/*
		 * MinecraftServer.getServer().addScheduledTask(new Runnable() {
		 * 
		 * @Override public void run() { try {
		 */
		try {
			connect(AdditionalNetwork.getDispatcher(), serverConnectionEvent.player);
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * } catch (IOException e) { getLogger().fatal(e); e.printStackTrace();
		 * } } });
		 */
	}

	@SubscribeEvent
	public void serverToClientDestroyed(ServerDisconnectionFromClientEvent serverConnectionEvent) {
		disconnect(((NetHandlerPlayServer) serverConnectionEvent.handler).playerEntity);
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
		public void handle(SharedNetwork networkHandler, IPlayerConnection playerConnection, Object object) {
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
		public void handle(SharedNetwork networkHandler, IPlayerConnection playerConnection, Object object) {
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
	public void sendUDP(GameProfile player, Object object) {
		IPlayerConnection connection = getActiveConnections().inverse().get(player);
		if (connection != null)
			server.sendToUDP(getActiveConnections().inverse().get(player).getID(), object);
	}

}
