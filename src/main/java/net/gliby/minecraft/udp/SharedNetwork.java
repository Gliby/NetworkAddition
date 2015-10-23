package net.gliby.minecraft.udp;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import net.gliby.minecraft.udp.packethandlers.IPacketHandler;
import net.gliby.minecraft.udp.packets.IAdditionalHandler;
import net.gliby.minecraft.udp.packets.MinecraftPacketWrapper;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.security.InnerAuth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraftforge.fml.relauncher.Side;

public abstract class SharedNetwork {

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

	protected void init(final SharedNetwork network, EndPoint endPoint) {
		endPoint.getKryo().register(PacketAuthentication.class);
		endPoint.getKryo().register(MinecraftPacketWrapper.class);
		endPoint.getKryo().register(InnerAuth.class);
		endPoint.getKryo().register(byte[].class);
		endPoint.getKryo().register(Class.class);
		endPoint.getKryo().register(S1CPacketEntityMetadata.class);
		endPoint.getKryo().register(ByteBuf.class);
		final HashMap<Class, IPacketHandler> packetHandlers = new HashMap<Class, IPacketHandler>();
		packetHandlers.put(MinecraftPacketWrapper.class, getClientDefaultPacketHandler());
		endPoint.addListener(new Listener() {

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
						handler.handle(network, connection, obj);
					else {
						network.getLogger().error("Wrong side for packet handler: " + handler + "(" + obj + ").");
					}
				} else {
					network.getLogger().debug("No packet handler found for: " + obj);
				}
			}
		});
	}

	public abstract Side getSide();

	public Logger getLogger() {
		return AdditionalNetwork.getInstance().getLogger();
	}

	public abstract void sendUDP(GameProfile player, Object object);
}
