package net.gliby.minecraft.udp;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

public interface IAdditionalHandler<T extends IMessage> {

	public void handle(T t, Side side);

}
