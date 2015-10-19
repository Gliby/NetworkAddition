package net.gliby.minecraft.udp;

public interface IConnectionInformation {

	/**
	 * Return UDP port.
	 * 
	 * @return
	 */
	public int getUDP();

	/**
	 * Return TCP port.
	 * 
	 * @return
	 */
	public int getTCP();

	public String getKey();

}
