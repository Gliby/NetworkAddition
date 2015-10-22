package net.gliby.minecraft.udp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import net.gliby.minecraft.udp.packethandlers.IPacketHandler;
import net.gliby.minecraft.udp.packets.IAdditionalHandler;
import net.gliby.minecraft.udp.packets.MinecraftPacketWrapper;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.security.Authenticator;
import net.gliby.minecraft.udp.security.Authenticator.IValidation;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S10PacketSpawnPainting;
import net.minecraft.network.play.server.S11PacketSpawnExperienceOrb;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S31PacketWindowProperty;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.network.play.server.S34PacketMaps;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S41PacketServerDifficulty;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S44PacketWorldBorder;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

//TODO Add security measures.
//TODO Don't allow more than 1 auth per player(each time wrong, multiply deny time by 1.2, clamp at 5 minutes.)

public class ServerNetworkHandler implements ISidedNetworkHandler {

	private HashMap<Class, IPacketHandler> externalPacketHandlers = new HashMap<Class, IPacketHandler>();

	public HashMap<Class, IPacketHandler> getExternalPacketHandlers() {
		return externalPacketHandlers;
	}

	public static List<Class> transplants = new ArrayList<Class>();

	{
		transplants.add(S02PacketChat.class);
//		transplants.add(S03PacketTimeUpdate.class);
//		transplants.add(S04PacketEntityEquipment.class);
		// transplants.add(S05PacketSpawnPosition.class);
		transplants.add(S06PacketUpdateHealth.class);
		// transplants.add(S07PacketRespawn.class);
		transplants.add(S08PacketPlayerPosLook.class);
		transplants.add(S09PacketHeldItemChange.class);
		transplants.add(S0APacketUseBed.class);
		transplants.add(S0BPacketAnimation.class);
		transplants.add(S0CPacketSpawnPlayer.class);
		transplants.add(S0DPacketCollectItem.class);
		transplants.add(S0EPacketSpawnObject.class);
		transplants.add(S0FPacketSpawnMob.class);
		transplants.add(S10PacketSpawnPainting.class);
		transplants.add(S11PacketSpawnExperienceOrb.class);
//		transplants.add(S12PacketEntityVelocity.class);
//		transplants.add(S13PacketDestroyEntities.class);
//		transplants.add(S14PacketEntity.class);
//		transplants.add(S14PacketEntity.S15PacketEntityRelMove.class);
//		transplants.add(S14PacketEntity.S16PacketEntityLook.class);
//		transplants.add(S14PacketEntity.S17PacketEntityLookMove.class);
//		transplants.add(S18PacketEntityTeleport.class);
//		transplants.add(S19PacketEntityHeadLook.class);
//		transplants.add(S19PacketEntityStatus.class);
		transplants.add(S1BPacketEntityAttach.class);
		// transplants.add(S1CPacketEntityMetadata.class);
//		transplants.add(S1DPacketEntityEffect.class);
		transplants.add(S1EPacketRemoveEntityEffect.class);
		transplants.add(S1FPacketSetExperience.class);
		transplants.add(S20PacketEntityProperties.class);
//		 transplants.add(S21PacketChunkData.class);
		 transplants.add(S22PacketMultiBlockChange.class);
		 transplants.add(S23PacketBlockChange.class);
		// transplants.add(S24PacketBlockAction.class);
		// transplants.add(S25PacketBlockBreakAnim.class);
//		 transplants.add(S26PacketMapChunkBulk.class);
		transplants.add(S27PacketExplosion.class);
		transplants.add(S28PacketEffect.class);
//		transplants.add(S29PacketSoundEffect.class);
		transplants.add(S2APacketParticles.class);
		transplants.add(S2BPacketChangeGameState.class);
		transplants.add(S2CPacketSpawnGlobalEntity.class);
		transplants.add(S2DPacketOpenWindow.class);
		transplants.add(S2EPacketCloseWindow.class);
		transplants.add(S2FPacketSetSlot.class);
		transplants.add(S30PacketWindowItems.class);
		transplants.add(S31PacketWindowProperty.class);
		transplants.add(S32PacketConfirmTransaction.class);
		transplants.add(S33PacketUpdateSign.class);
		transplants.add(S34PacketMaps.class);
		transplants.add(S35PacketUpdateTileEntity.class);
		transplants.add(S36PacketSignEditorOpen.class);
		transplants.add(S37PacketStatistics.class);
		transplants.add(S38PacketPlayerListItem.class);
		transplants.add(S39PacketPlayerAbilities.class);
		transplants.add(S3APacketTabComplete.class);
		transplants.add(S3BPacketScoreboardObjective.class);
		transplants.add(S3CPacketUpdateScore.class);
		transplants.add(S3DPacketDisplayScoreboard.class);
		transplants.add(S3EPacketTeams.class);
		// transplants.add(S3FPacketCustomPayload.class);
		// transplants.add(S40PacketDisconnect.class);
		transplants.add(S41PacketServerDifficulty.class);
		transplants.add(S42PacketCombatEvent.class);
		transplants.add(S43PacketCamera.class);
		transplants.add(S44PacketWorldBorder.class);
		// transplants.add(S45PacketTitle.class);
		// transplants.add(S46PacketSetCompressionLevel.class);
		// transplants.add(S47PacketPlayerListHeaderFooter.class);
		// transplants.add(S48PacketResourcePackSend.class);
		// transplants.add(S49PacketUpdateEntityNBT.class);

	}

	protected void init(final ISidedNetworkHandler networkHandler, EndPoint point) {

		// Log.setLogger(new AnotherLogger(networkHandler.getLogger()));
		point.getKryo().register(PacketAuthentication.class);
		point.getKryo().register(MinecraftPacketWrapper.class);
		point.getKryo().register(InnerAuth.class);
		point.getKryo().register(byte[].class);
		point.getKryo().register(Class.class);
		point.getKryo().register(ByteBuf.class);
		for (Class clazz : transplants) {
			point.getKryo().register(clazz);
		}

		packetHandlers = new HashMap<Class, IPacketHandler>();
		for (Entry<Class, IPacketHandler> entry : externalPacketHandlers.entrySet()) {
			// point.getKryo().register(entry.getKey());
			/*
			 * for (Field field : entry.getKey().getDeclaredFields()) {
			 * point.getKryo().register(field.getClass()); }
			 */
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
	public void sendUDP(GameProfile player, Object object) {
		IPlayerConnection connection = getActiveConnections().inverse().get(player);
		if (connection != null)
			server.sendToUDP(getActiveConnections().inverse().get(player).getID(), object);
	}

}
