package net.gliby.minecraft.udp;

import java.io.IOException;

import io.netty.buffer.Unpooled;
import net.gliby.minecraft.udp.HijackedNetPlayerHandler.PacketEvent;
import net.gliby.minecraft.udp.packets.DataWatcherUpdate;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public class PacketInterceptEvent {

	private ISidedNetworkHandler networkHandler;

	public PacketInterceptEvent(ISidedNetworkHandler network) {
		this.networkHandler = network;
	}

	@SubscribeEvent
	public void event(PacketEvent.Send send) {
		if (send.packet instanceof S1CPacketEntityMetadata) {
			PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
			try {
				send.packet.writePacketData(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			networkHandler.sendUDP(send.player, new DataWatcherUpdate(buffer.array()));
			send.setCanceled(true);
		}
	}
}
