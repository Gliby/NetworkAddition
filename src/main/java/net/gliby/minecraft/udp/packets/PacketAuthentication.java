package net.gliby.minecraft.udp.packets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister.Pack;

import io.netty.buffer.ByteBuf;
import net.gliby.minecraft.udp.AdditionalNetwork;
import net.gliby.minecraft.udp.IConnectionInformation;
import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.ISidedNetworkHandler;
import net.gliby.minecraft.udp.ServerNetworkHandler;
import net.gliby.minecraft.udp.client.ClientNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 *
 */
public class PacketAuthentication extends MinecraftPacket
		implements IMessageHandler<PacketAuthentication, IMessage>, IAdditionalHandler<PacketAuthentication> {

	public String key;
	public int udp, tcp;

	public PacketAuthentication() {
	}

	public PacketAuthentication(String key, int tcp, int udp) {
		this.key = key;
		this.tcp = tcp;
		this.udp = udp;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.udp = buf.readInt();
		this.tcp = buf.readInt();
		this.key = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(udp);
		buf.writeInt(tcp);
		ByteBufUtils.writeUTF8String(buf, key);
	}

	@Override
	public IMessage onMessage(final PacketAuthentication message, MessageContext ctx) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				try {
					AdditionalNetwork ad = AdditionalNetwork.getInstance();
					ad.getProxy().connect(AdditionalNetwork.getDispatcher(),
							FMLClientHandler.instance().getClientPlayerEntity(),
							new ConnectionInformation(message.key, message.udp, message.tcp));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		return null;
	}

	public class ConnectionInformation implements IConnectionInformation {
		public final String key;
		public final int udp, tcp;

		public ConnectionInformation(String key, int udp, int tcp) {
			this.key = key;
			this.udp = udp;
			this.tcp = tcp;
		}

		@Override
		public int getUDP() {
			return udp;
		}

		@Override
		public int getTCP() {
			return tcp;
		}

		@Override
		public String getKey() {
			return key;
		}
	}

	@Override
	public void handle(ISidedNetworkHandler networkHandler, IPlayerConnection playerConnection, Object object) {
		ClientNetworkHandler clientNetworkHandler = (ClientNetworkHandler) networkHandler;
		clientNetworkHandler.setAuthenticated(true);
	}
}