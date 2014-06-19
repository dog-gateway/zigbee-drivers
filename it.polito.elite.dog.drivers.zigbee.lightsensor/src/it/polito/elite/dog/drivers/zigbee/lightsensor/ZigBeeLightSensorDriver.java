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
package it.polito.elite.dog.drivers.zigbee.lightsensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.LightSensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import org.energy_home.jemma.ah.cluster.zigbee.measurement.IlluminanceMeasurementServer;

import org.osgi.framework.BundleContext;

/**
 * <p>
 * This class implements the LightSensor driver for the ZigBee network. It takes
 * care of matching and attaching devices of type {@link LightSensor} and of
 * delegating their management to suitable driver instances (
 * {@link ZigBeeLightSensorDriverInstance}).
 * </p>
 * 
 * @author bonino
 * 
 */
public class ZigBeeLightSensorDriver extends ZigBeeDeviceDriver

{
	protected static final String logId = "[ZigBeeLightSensorDriver]: ";

	/**
	 * Empty constructor, initializes inner data structures.
	 */
	public ZigBeeLightSensorDriver()
	{
		super();

		// setup supported clusters
		this.serverClusters.add(IlluminanceMeasurementServer.class.getName()
				.replace("Server", ""));

		// setup categories
		this.driverInstanceClass = ZigBeeLightSensorDriverInstance.class;
		this.deviceMainClass = LightSensor.class.getSimpleName();

	}

	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeLightSensorDriverInstance(zigBeeNetwork, device,
				context, reportingTimeSeconds);
	}
}
