package net.gliby.minecraft.udp.packets;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 *
 */
public class PacketAuthentication extends MinecraftPacket implements IMessageHandler<PacketAuthentication, IMessage> {

	private String key;

	public PacketAuthentication() {
	}

	public PacketAuthentication(String key) {
		this.key = key;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.key = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, key);
	}

	@Override
	public IMessage onMessage(final PacketAuthentication message, MessageContext ctx) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {

			@Override
			public void run() {
				
			}
		});
		return null;
	}
}