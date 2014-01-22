/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.network.interfaces;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;

/**
 * @author bonino
 * 
 */
public interface ApplianceDiscoveryListener
{
	/**
	 * Called whenever an unknown appliance is detected at the network level.
	 * Provides all the available information about the appliance, including the
	 * serial number and a pointer to the live appliance managed by the
	 * underlying ZigBee network layer.
	 * 
	 * @param applianceInfo The {@link ZigBeeApplianceInfo} instance describing the newly detected appliance.
	 */
	public void applianceDiscovered(ZigBeeApplianceInfo applianceInfo);
}
