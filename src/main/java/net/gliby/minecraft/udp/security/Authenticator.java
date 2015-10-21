package net.gliby.minecraft.udp.security;

import java.util.HashMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.gliby.minecraft.udp.SessionIdentifierGenerator;

public class Authenticator {

	private BiMap<GameProfile, String> awaitingAuthentication;
	private HashMap<GameProfile, String> authenticated;

	private SessionIdentifierGenerator secureId;

	public Authenticator() {
		secureId = new SessionIdentifierGenerator();
		authenticated = new HashMap<GameProfile, String>();
		awaitingAuthentication = HashBiMap.create();
	}

	public void removeAuthentication(GameProfile profile) {
		authenticated.remove(profile);
		awaitingAuthentication.remove(profile);
	}

	public String getAuthenticationKey(GameProfile gameProfile) {
		if (!authenticated.containsKey(gameProfile)) {
			String key = secureId.nextId(130, 32);
			awaitingAuthentication.put(gameProfile, key);
			return key;
		}
		return null;
	}

	public interface IValidation {
		public GameProfile getOwner();
	}

	public IValidation getValidation(String key) {
		final GameProfile profile;
		if (key.length() == 26 && (profile = getOwner(key)) != null) {
			return new IValidation() {

				@Override
				public GameProfile getOwner() {
					return profile;
				}
			};
		}
		return null;
	}

	public GameProfile getOwner(String key) {
		BiMap<String, GameProfile> reversed = awaitingAuthentication.inverse();
		GameProfile gameProfile = reversed.get(key);
		return gameProfile;
	}

}
