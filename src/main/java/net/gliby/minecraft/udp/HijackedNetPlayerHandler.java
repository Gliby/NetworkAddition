package net.gliby.minecraft.udp;

import java.util.concurrent.Callable;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;

public class HijackedNetPlayerHandler extends NetHandlerPlayServer {

	public static class PacketEvent extends Event {
		public NetworkManager networkManager;
		public EntityPlayer player;

		public PacketEvent(NetworkManager networkManager, EntityPlayerMP player) {
			this.networkManager = networkManager;
			this.player = player;
		}

		@Cancelable
		public static class Send extends PacketEvent {
			public Packet packet;

			public Send(NetworkManager manager, EntityPlayerMP player, Packet packet) {
				super(manager, player);
				this.packet = packet;
			}
		}
	}

	private EventBus bus;

	public EventBus bus() {
		return bus;
	}

	public HijackedNetPlayerHandler(MinecraftServer server, NetworkManager networkManagerIn, EntityPlayerMP playerIn) {
		super(server, networkManagerIn, playerIn);
		this.bus = new EventBus();
	}

	@Override
	public void sendPacket(final Packet packetIn) {
		if (!bus().post(new PacketEvent.Send(getNetworkManager(), playerEntity, packetIn))) {
			super.sendPacket(packetIn);
		}
	}

}
