package net.gliby.minecraft.udp;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class SessionIdentifierGenerator {
	private SecureRandom random;

	public SessionIdentifierGenerator() {
		this.random = new SecureRandom();
	}

	public String nextId() {
		return new BigInteger(130, random).toString(32);
	}
}