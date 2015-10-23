package net.gliby.minecraft.udp.packets;

import java.io.IOException;

import com.ibm.icu.util.TimeZone.SystemTimeZoneType;

import io.netty.buffer.Unpooled;
import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.SharedNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class MinecraftPacketWrapper implements IAdditionalHandler<MinecraftPacketWrapper> {

	public Class packet;
	public byte[] information;

	public MinecraftPacketWrapper() {
	}

	@Override
	public void handle(SharedNetwork networkHandler, IPlayerConnection playerConnection,
			final MinecraftPacketWrapper object) {
		final Minecraft mc = Minecraft.getMinecraft();
		if (object.packet != null) {
			mc.addScheduledTask(new Runnable() {

				@Override
				public void run() {
					Packet packet = null;
					try {
						packet = (Packet) object.packet.newInstance();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					try {
						packet.readPacketData(new PacketBuffer(Unpooled.copiedBuffer(object.information)));
					} catch (IOException e) {
						e.printStackTrace();
					}
					packet.processPacket(mc.getNetHandler());
				}
			});
		}

	}

}
