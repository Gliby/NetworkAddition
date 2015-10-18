package net.gliby.minecraft.udp;

import com.esotericsoftware.kryonet.Connection;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerConnection extends Connection {

	private GameProfile gameProfile;

	public GameProfile getGameProfile() {
		return gameProfile;
	}

	private EntityPlayer player;

	public EntityPlayer getPlayer() {
		return player;
	}
}
