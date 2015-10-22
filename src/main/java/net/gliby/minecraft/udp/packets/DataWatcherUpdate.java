package net.gliby.minecraft.udp.packets;

import java.io.IOException;

import javax.xml.crypto.Data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.ISidedNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

public class DataWatcherUpdate implements IAdditionalHandler<DataWatcherUpdate> {

	byte[] buffer;

	public DataWatcherUpdate(byte[] buf) {
		this.buffer = buf;
	}

	public DataWatcherUpdate() {
	}

	@Override
	public void handle(ISidedNetworkHandler networkHandler, IPlayerConnection playerConnection,
			final DataWatcherUpdate object) {
		System.out.println("ey");
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {

			@Override
			public void run() {
				Minecraft mc = Minecraft.getMinecraft();
				S1CPacketEntityMetadata metadataPacket = new S1CPacketEntityMetadata();
				try {
					metadataPacket.readPacketData(new PacketBuffer(Unpooled.copiedBuffer(object.buffer)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				mc.getNetHandler().handleEntityMetadata(metadataPacket);

			}
		});
	}

}
