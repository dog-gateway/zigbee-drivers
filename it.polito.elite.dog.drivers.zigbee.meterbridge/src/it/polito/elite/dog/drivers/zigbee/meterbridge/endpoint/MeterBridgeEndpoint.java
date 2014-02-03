/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.meterbridge.endpoint;

import it.polito.elite.dog.drivers.zigbee.meterbridge.appliance.BridgedMeteringAppliance;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.lib.EndPoint;


/**
 * @author bonino
 *
 */
public class MeterBridgeEndpoint extends EndPoint
{

	public MeterBridgeEndpoint(String type, BridgedMeteringAppliance appliance) throws ApplianceException
	{
		super(type);
		this.appliance = appliance;
	}

}
