package net.gliby.minecraft.udp;

import java.io.IOException;

import io.netty.buffer.Unpooled;
import net.gliby.minecraft.udp.HijackedNetPlayerHandler.PacketEvent;
import net.gliby.minecraft.udp.packets.MinecraftPacketWrapper;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PacketInterceptEvent {

	private ISidedNetworkHandler networkHandler;

	public PacketInterceptEvent(ISidedNetworkHandler network) {
		this.networkHandler = network;
	}

	@SubscribeEvent
	public void event(PacketEvent.Send send) {
		if (ServerNetworkHandler.transplants.contains(send.packet.getClass())) {
			PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
			try {
				send.packet.writePacketData(packetBuffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			MinecraftPacketWrapper wrapper = new MinecraftPacketWrapper();
			wrapper.packet = send.packet.getClass();
			wrapper.information = packetBuffer.array();
			networkHandler.sendUDP(send.player, wrapper);
			
			send.setCanceled(true);
		}
	}
}
