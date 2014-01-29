/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.doorsensor;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.DoorSensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffClient;
import it.telecomitalia.ah.cluster.zigbee.security.IASZoneServer;
import it.telecomitalia.ah.hac.lib.ext.IConnectionAdminService;

/**
 * @author bonino
 *
 */
public class ZigBeeDoorWindowSensorDriver extends ZigBeeDeviceDriver
{
	
	private AtomicReference<IConnectionAdminService> connectionAdmin;
	
	public ZigBeeDoorWindowSensorDriver()
	{
		super();
		
		//setup supported clusters
		this.serverClusters.add(IASZoneServer.class.getName().replace("Server",""));
		this.clientClusters.add(OnOffClient.class.getName().replace("Client",""));
		
		//setup categories
		this.deviceCategories.add(DoorSensor.class.getName());
		this.deviceMainClass = DoorSensor.class.getSimpleName();
		
		//set up atomic reference
		this.connectionAdmin = new AtomicReference<IConnectionAdminService>();
		
	}
	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeDoorWindowSensorDriverInstance(zigBeeNetwork, device, context, reportingTimeSeconds, this.connectionAdmin.get());
	}
	
	//additional bindings
	public void addedConnectionAdminService(
			IConnectionAdminService connectionAdmin)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, "Added ConnectionAdmin service");

		// store the network driver reference
		this.connectionAdmin.set(connectionAdmin);
		
		//update references
		for(ZigBeeDriverInstance instance : this.managedInstances.values())
		{
			((ZigBeeDoorWindowSensorDriverInstance)instance).setConnectionAdmin(this.connectionAdmin.get());
		}

	}

	public void removedConnectionAdminService(IConnectionAdminService gateway)
	{
		// null the network freeing the old reference for gc
		if (this.connectionAdmin.compareAndSet(gateway, null))
		{
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG, "Removed ConnectionAdmin service");
			
			//update references
			for(ZigBeeDriverInstance instance : this.managedInstances.values())
			{
				((ZigBeeDoorWindowSensorDriverInstance)instance).setConnectionAdmin(null);
			}
		}
	}

}
