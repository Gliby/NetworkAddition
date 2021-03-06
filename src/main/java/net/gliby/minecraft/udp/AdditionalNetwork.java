package net.gliby.minecraft.udp;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import net.gliby.minecraft.udp.client.ClientNetworkHandler;
import net.gliby.minecraft.udp.packets.MinecraftPacketWrapper;
import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.gliby.minecraft.udp.server.ServerNetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
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

	@Instance
	public static AdditionalNetwork instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		this.instance = this;
		this.logger = event.getModLog();
		registerPacket(PacketAuthentication.class, PacketAuthentication.class, Side.CLIENT);
		// TODO Make these register with Kyro properly.
		// proxy.getExternalPacketHandlers().put(PacketAuthentication.class,
		// proxy.getClientDefaultPacketHandler());
		// proxy.getExternalPacketHandlers().put(MinecraftPacketWrapper.class,
		// proxy.getClientDefaultPacketHandler());
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
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

	ServerNetworkHandler serverNetwork;

	@EventHandler
	public void serverStart(FMLServerStartedEvent serverEvent) {
		serverNetwork = new ServerNetworkHandler();
		FMLCommonHandler.instance().bus().register(serverNetwork);
		try {
			serverNetwork.start(this);
		} catch (IOException e) {
			getLogger().fatal(e);
			e.printStackTrace();
		}
	}

	@EventHandler
	public void serverStop(FMLServerStoppingEvent serverEvent) {
		FMLCommonHandler.instance().bus().unregister(serverNetwork);
		serverNetwork.stop(this);
	}

	private Logger logger;

	public Logger getLogger() {
		return logger;
	}

	public static AdditionalNetwork getInstance() {
		return instance;
	}

	ClientNetworkHandler clientNetwork;

	public ClientNetworkHandler createClientNetwork() {
		if (clientNetwork != null) {
			FMLCommonHandler.instance().bus().unregister(clientNetwork);
		}
		FMLCommonHandler.instance().bus().register(clientNetwork = new ClientNetworkHandler());
		return clientNetwork;
	}
}
