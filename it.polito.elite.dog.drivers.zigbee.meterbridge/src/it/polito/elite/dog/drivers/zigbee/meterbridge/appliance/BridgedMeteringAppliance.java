/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.meterbridge.appliance;

import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IApplianceDescriptor;
import it.telecomitalia.ah.hac.lib.Appliance;
import it.telecomitalia.ah.hac.lib.ApplianceDescriptor;

import java.util.Dictionary;

/**
 * @author bonino
 *
 */
public class BridgedMeteringAppliance extends Appliance
{

	private ApplianceDescriptor descriptor;
	
	public BridgedMeteringAppliance(String pid, Dictionary<?,?> config)
			throws ApplianceException
	{
		super(pid, config);

		this.descriptor = new ApplianceDescriptor("it.polito.elite.drivers.zigbee", pid);
	}

	@Override
	public IApplianceDescriptor getDescriptor()
	{
		return this.descriptor;
	}
}
