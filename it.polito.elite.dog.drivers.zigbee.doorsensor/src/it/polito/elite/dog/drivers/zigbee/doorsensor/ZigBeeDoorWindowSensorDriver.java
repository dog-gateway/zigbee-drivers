/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.doorsensor;

import org.osgi.framework.BundleContext;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.DoorSensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffClient;
import it.telecomitalia.ah.cluster.zigbee.security.IASZoneServer;

/**
 * @author bonino
 *
 */
public class ZigBeeDoorWindowSensorDriver extends ZigBeeDeviceDriver
{

	protected static final String logId = "[ZigBeeDoorSensorDriver]: ";
	
	public ZigBeeDoorWindowSensorDriver()
	{
		super();
		
		//setup supported clusters
		this.serverClusters.add(IASZoneServer.class.getName().replace("Server",""));
		this.clientClusters.add(OnOffClient.class.getName().replace("Client",""));
		
		//setup categories
		this.deviceCategories.add(DoorSensor.class.getName());
		this.deviceMainClass = DoorSensor.class.getSimpleName();
		
	}
	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeDoorWindowSensorDriverInstance(zigBeeNetwork, device, context, reportingTimeSeconds);
	}

}
