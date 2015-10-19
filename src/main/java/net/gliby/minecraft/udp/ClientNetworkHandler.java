package net.gliby.minecraft.udp;

import java.io.IOException;

import org.apache.logging.log4j.core.net.UDPSocketServer;

import net.gliby.minecraft.udp.packets.PacketAuthentication;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class ClientNetworkHandler extends ServerNetworkHandler
		implements IMessageHandler<PacketAuthentication, IMessage> {

	@Override
	public IMessage onMessage(final PacketAuthentication message, MessageContext ctx) {
		this.key = message.key;
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				try {
					connect(AdditionalNetwork.getDispatcher(), FMLClientHandler.instance().getClientPlayerEntity());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		return null;
	}

	public String key;

	// Temporary
	public String getHost() {
		return "localhost";
	}

	@Override
	public void connect(SimpleNetworkWrapper networkDispatcher, EntityPlayer player) throws IOException {
		/*
		 * final Client client = new Client(); client.addListener(new Listener()
		 * {
		 * 
		 * @Override public void connected(Connection connection) { }
		 * 
		 * }); client.start(); client.connect(5000, getHost(),
		 * this.getTCPPort(), this.getUDPPort());
		 */
	}

	@Override
	public void disconnect(EntityPlayer player) {

	}

}
