package net.gliby.minecraft.udp;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerDisconnectionFromClientEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A simple UDP implementation for Minecraft.
 *
 */
//TODO Get stuff working.
@Mod(name = UDP.NAME, modid = UDP.MODID, version = UDP.VERSION)
public class UDP {

	public static final String NAME = "Gliby's Network Addition";
	public static final String MODID = "glibyudp";
	public static final String VERSION = "1.0";

	@SidedProxy(serverSide = "net.gliby.minecraft.udp.ServerNetworkHandler", clientSide = "net.gliby.minecraft.udp.ClientNetworkHandler")
	public static ServerNetworkHandler proxy;

	@EventHandler
	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void clientToServerEstablished(ClientConnectedToServerEvent clientConnectionEvent) {
		proxy.connect(FMLClientHandler.instance().getClientPlayerEntity());
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void clientToServerDestroyed(ClientDisconnectionFromServerEvent clientConnectionEvent) {
		proxy.disconnect(FMLClientHandler.instance().getClientPlayerEntity());
	}

	@EventHandler
	public void serverStart(FMLServerStartedEvent serverEvent) {
		proxy.start();
	}

	@SubscribeEvent
	public void serverToClientEstablished(ServerConnectionFromClientEvent serverConnectionEvent) {
		proxy.connect(((NetHandlerPlayServer) serverConnectionEvent.handler).playerEntity);
	}

	@SubscribeEvent
	public void serverToClientDestroyed(ServerDisconnectionFromClientEvent serverConnectionEvent) {
		proxy.disconnect(((NetHandlerPlayServer) serverConnectionEvent.handler).playerEntity);
	}
}
