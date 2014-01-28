/*
 * Dog 2.0 - ZigBee EnergyAndPowerMeter Driver
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
package it.polito.elite.dog.drivers.zigbee.energyandpowermeter;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.EnergyAndPowerMeter;
import it.polito.elite.dog.drivers.zigbee.device.ZigBeeDeviceDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.metering.SimpleMeteringServer;

import org.osgi.framework.BundleContext;

/**
 * @author bonino
 * 
 */

public class ZigBeeEnergyAndPowerMeterDriver extends ZigBeeDeviceDriver
{
protected static final String logId = "[ZigBeeEnergyAndPowerMeterDriver]: ";
	
	public ZigBeeEnergyAndPowerMeterDriver()
	{
		super();
		
		//setup supported clusters
		this.serverClusters.add(SimpleMeteringServer.class.getName().replace("Server",""));

		
		//setup categories
		this.deviceCategories.add(EnergyAndPowerMeter.class.getName());
		this.deviceMainClass = EnergyAndPowerMeter.class.getSimpleName();
		
	}
	@Override
	public ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		// TODO Auto-generated method stub
		return new ZigBeeEnergyAndPowerMeterDriverInstance(zigBeeNetwork, device, context, reportingTimeSeconds);
	}
}
