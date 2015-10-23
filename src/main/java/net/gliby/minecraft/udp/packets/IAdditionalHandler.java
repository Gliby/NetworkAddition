package net.gliby.minecraft.udp.packets;

import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.SharedNetwork;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

public interface IAdditionalHandler<T> {

	public void handle(SharedNetwork networkHandler, IPlayerConnection playerConnection, T object);

}
