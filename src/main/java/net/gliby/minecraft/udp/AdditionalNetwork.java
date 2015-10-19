package net.gliby.minecraft.udp;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerDisconnectionFromClientEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A simple TCP/UDP implementation for Minecraft.
 *
 */
@Mod(name = AdditionalNetwork.NAME, modid = AdditionalNetwork.MODID, version = AdditionalNetwork.VERSION)
public class AdditionalNetwork {

	public static final String NAME = "Gliby's Additional Network";
	public static final String MODID = "glibysnetwork";
	public static final String VERSION = "1.0";

	@SidedProxy(serverSide = "net.gliby.minecraft.udp.ServerNetworkHandler", clientSide = "net.gliby.minecraft.udp.ClientNetworkHandler")
	public static ServerNetworkHandler proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		this.logger = event.getModLog();
		registerPacket(PacketAuthentication.class, PacketAuthentication.class, Side.CLIENT);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
	}

	private static final SimpleNetworkWrapper DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

	/**
	 * @return the dispatcher
	 */
	public static SimpleNetworkWrapper getDispatcher() {
		return DISPATCHER;
	}

	private static int packetIndex = 0;

	public void registerPacket(Class handler, Class packet, Side side) {
		getDispatcher().registerMessage(packet, handler, packetIndex++, side);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void clientToServerEstablished(ClientConnectedToServerEvent clientConnectionEvent) {
	/*	try {
			EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
			if (player != null)
				proxy.connect(getDispatcher(), player);
		} catch (IOException e) {
			getLogger().fatal(e);
			e.printStackTrace();
		}*/
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void clientToServerDestroyed(ClientDisconnectionFromServerEvent clientConnectionEvent) {
		EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
		if (player != null)
			proxy.disconnect(player);
	}

	@EventHandler
	public void serverStart(FMLServerStartedEvent serverEvent) {
		try {
			proxy.start(this);
		} catch (IOException e) {
			getLogger().fatal(e);
			e.printStackTrace();
		}
	}

	private Logger logger;

	public Logger getLogger() {
		return logger;
	}

	@SubscribeEvent
	public void serverToClientEstablished(ServerConnectionFromClientEvent serverConnectionEvent) {
		try {
			proxy.connect(getDispatcher(), ((NetHandlerPlayServer) serverConnectionEvent.handler).playerEntity);
		} catch (IOException e) {
			getLogger().fatal(e);
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void serverToClientDestroyed(ServerDisconnectionFromClientEvent serverConnectionEvent) {
		proxy.disconnect(((NetHandlerPlayServer) serverConnectionEvent.handler).playerEntity);
	}
}
