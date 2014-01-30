/*
 * Dog 2.0 - ZigBee LightSensor Driver
 * 
 * Copyright 2013 Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.drivers.zigbee.movementsensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.MovementSensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.measurement.OccupancySensingServer;
import it.telecomitalia.ah.hac.lib.ext.IConnectionAdminService;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeMovementSensorDriver extends ZigBeeDeviceDriver

{
	private AtomicReference<IConnectionAdminService> connectionAdmin;

	public ZigBeeMovementSensorDriver()
	{
		super();

		// setup supported clusters
		this.serverClusters.add(OccupancySensingServer.class.getName().replace(
				"Server", ""));

		// setup categories
		this.deviceCategories.add(MovementSensor.class.getName());
		this.deviceMainClass = MovementSensor.class.getSimpleName();

		// set up atomic reference
		this.connectionAdmin = new AtomicReference<IConnectionAdminService>();

	}

	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeMovementSensorDriverInstance(zigBeeNetwork, device,
				context, reportingTimeSeconds, this.connectionAdmin.get());
	}

	// additional bindings
	public void addedConnectionAdminService(
			IConnectionAdminService connectionAdmin)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG,
					"Added ConnectionAdmin service");

		// store the network driver reference
		this.connectionAdmin.set(connectionAdmin);

		// update references
		for (ZigBeeDriverInstance instance : this.managedInstances.values())
		{
			((ZigBeeMovementSensorDriverInstance) instance)
					.setConnectionAdmin(this.connectionAdmin.get());
		}

	}

	public void removedConnectionAdminService(IConnectionAdminService gateway)
	{
		// null the network freeing the old reference for gc
		if (this.connectionAdmin.compareAndSet(gateway, null))
		{
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG,
						"Removed ConnectionAdmin service");

			// update references
			for (ZigBeeDriverInstance instance : this.managedInstances.values())
			{
				((ZigBeeMovementSensorDriverInstance) instance)
						.setConnectionAdmin(null);
			}
		}
	}
}
