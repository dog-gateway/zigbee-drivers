/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.network;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;

/**
 * @author bonino
 *
 */
public abstract class ZigBeeDriver
{
	protected ZigBeeDriverInfo driverInfo;

	public ZigBeeDriverInfo getDriverInfo()
	{
		return driverInfo;
	}

	public void setDriverInfo(ZigBeeDriverInfo driverInfo)
	{
		this.driverInfo = driverInfo;
	}
	
	
}
