/*
 * Dog 2.0 - ZigBee Door and Window Sensor Driver
 * 
 * Copyright 2014 Dario Bonino 
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
package it.polito.elite.dog.drivers.zigbee.doorwindowsensor;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.DoorSensor;
import it.polito.elite.dog.core.library.model.devicecategory.WindowSensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffClient;
import it.telecomitalia.ah.cluster.zigbee.security.IASZoneServer;
import it.telecomitalia.ah.hac.lib.ext.IConnectionAdminService;

/**
 * <p>
 * This class implements the Door and Window sensor driver for the ZigBee
 * network. It takes care of matching and attaching devices of type
 * {@link DoorSensor} and {@link WindowSensor} and of delegating their
 * management to suitable driver instances (
 * {@link ZigBeeDoorWindowSensorDriverInstance}).
 * </p>
 * 
 * <p>
 * Door and Window sensors, in ZigBee, typically expose an IASZone server
 * cluster, whose attribute ZoneStatus signals whether the given door/window is
 * open/close, and an OnOffClient cluster used to control bound devices exposing
 * a suitable OnOff server cluster.
 * </p>
 * 
 * <p>
 * Each driver instance registers a dedicated virtual appliance that exposes an
 * OnOff server cluster on the ZigBee network. Such a cluster is
 * programmatically bound to the OnOff client cluster exposed by the device in
 * order to capture door/window events.
 * </p>
 * 
 * @author bonino
 * 
 */
public class ZigBeeDoorWindowSensorDriver extends ZigBeeDeviceDriver
{
	// a reference to the connection admin service used to bind the device and
	// the driver clusters
	private AtomicReference<IConnectionAdminService> connectionAdmin;

	/**
	 * Empty constructor, initializes inner data structures, only.
	 */
	public ZigBeeDoorWindowSensorDriver()
	{
		super();

		// setup supported clusters
		this.serverClusters.add(IASZoneServer.class.getName().replace("Server",
				""));
		this.clientClusters.add(OnOffClient.class.getName().replace("Client",
				""));

		// setup categories
		this.deviceCategories.add(DoorSensor.class.getName());
		this.deviceMainClass = DoorSensor.class.getSimpleName();

		// set up atomic reference
		this.connectionAdmin = new AtomicReference<IConnectionAdminService>();

	}

	/**
	 * Creates an instance of {@link ZigBeeDoorWindowSensorDriverInstance} that
	 * will manage a specific device under attachment.
	 */
	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		return new ZigBeeDoorWindowSensorDriverInstance(zigBeeNetwork, device,
				context, reportingTimeSeconds, this.connectionAdmin.get());
	}

	// additional bindings

	/**
	 * Bind the {@link IConnectionAdminService} service available in the
	 * framework. Updates all references in already existing driver instances.
	 * 
	 * @param connectionAdmin
	 */
	public void addedConnectionAdminService(
			IConnectionAdminService connectionAdmin)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG,
					"Added ConnectionAdmin service");

		// store the network driver reference
		this.connectionAdmin.set(connectionAdmin);

		// update references to the connection admin
		for (ZigBeeDriverInstance instance : this.managedInstances.values())
		{
			((ZigBeeDoorWindowSensorDriverInstance) instance)
					.setConnectionAdmin(this.connectionAdmin.get());
		}

	}

	/**
	 * Unbind the {@link IConnectionAdminService} service when it is no more available in the
	 * framework. Updates all references in already existing driver instances.
	 * 
	 * @param connectionAdmin
	 */
	public void removedConnectionAdminService(IConnectionAdminService connectionAdmin)
	{
		// null the network freeing the old reference for gc
		if (this.connectionAdmin.compareAndSet(connectionAdmin, null))
		{
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG,
						"Removed ConnectionAdmin service");

			// update references
			for (ZigBeeDriverInstance instance : this.managedInstances.values())
			{
				((ZigBeeDoorWindowSensorDriverInstance) instance)
						.setConnectionAdmin(null);
			}
		}
	}

}
