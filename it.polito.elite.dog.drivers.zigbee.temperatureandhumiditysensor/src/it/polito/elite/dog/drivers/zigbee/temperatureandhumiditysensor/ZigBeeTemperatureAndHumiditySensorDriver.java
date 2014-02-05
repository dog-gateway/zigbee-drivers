/*
 * Dog 2.0 - ZigBee TemperatureAndHumiditySensor Driver
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
package it.polito.elite.dog.drivers.zigbee.temperatureandhumiditysensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.TemperatureAndHumiditySensor;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.measurement.RelativeHumidityMeasurementServer;
import it.telecomitalia.ah.cluster.zigbee.measurement.TemperatureMeasurementServer;

import org.osgi.framework.BundleContext;

/**
 * <p>
 * This class implements the Temperature and Humidity sensor driver for the ZigBee network. It takes
 * care of matching and attaching devices of type {@link TemperatureAndHumiditySensor} and of
 * delegating their management to suitable driver instances (
 * {@link ZigBeeTemperatureAndHumiditySensorInstance}).
 * </p>
 * @author bonino
 * 
 * 
 */
public class ZigBeeTemperatureAndHumiditySensorDriver extends ZigBeeDeviceDriver
{
protected static final String logId = "[ZigBeeDoorSensorDriver]: ";
	
	public ZigBeeTemperatureAndHumiditySensorDriver()
	{
		super();
		
		//setup supported clusters
		this.serverClusters.add(TemperatureMeasurementServer.class.getName().replace("Server",""));
		this.serverClusters.add(RelativeHumidityMeasurementServer.class.getName().replace("Server",""));
		
		//setup categories
		this.driverInstanceClass = ZigBeeTemperatureAndHumiditySensorDriverInstance.class;
		this.deviceMainClass = TemperatureAndHumiditySensor.class.getSimpleName();
		
	}
	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeTemperatureAndHumiditySensorDriverInstance(zigBeeNetwork, device, context, reportingTimeSeconds);
	}
}
