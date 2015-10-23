package net.gliby.minecraft.udp.server;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class SessionIdentifierGenerator {
	private SecureRandom random;

	public SessionIdentifierGenerator() {
		this.random = new SecureRandom();
	}

	public String nextId(int bits, int maxLength) {
		return new BigInteger(bits, random).toString(maxLength);
	}
}