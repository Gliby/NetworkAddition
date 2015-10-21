package net.gliby.minecraft.udp;

import com.esotericsoftware.kryonet.Connection;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;

public class PlayerConnection extends Connection {

	private GameProfile gameProfile;
	public INetHandler netHandler;

	public GameProfile getGameProfile() {
		return gameProfile;
	}

	public boolean isValid() {
		return gameProfile != null;
	}

	public void validate(GameProfile profile) {
		this.gameProfile = profile;
	}
}
