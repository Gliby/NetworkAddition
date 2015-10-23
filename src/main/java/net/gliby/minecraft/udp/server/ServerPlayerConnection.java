package net.gliby.minecraft.udp.server;

import com.esotericsoftware.kryonet.Connection;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.IPlayerConnection;
import net.gliby.minecraft.udp.security.Authenticator.IValidation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;

public class ServerPlayerConnection extends Connection implements IPlayerConnection {

	private GameProfile gameProfile;

	public GameProfile getGameProfile() {
		return gameProfile;
	}

	@Override
	public boolean isValid() {
		return gameProfile != null;
	}

	public void validate(IValidation validation) {
		this.gameProfile = validation.getOwner();
	}

}
