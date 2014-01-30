/*
 * Dog 2.0 - ZigBee DoorSensor Driver
 * 
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
package it.polito.elite.dog.drivers.zigbee.movementsensor.appliance;

import java.util.Dictionary;

import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IApplianceDescriptor;
import it.telecomitalia.ah.hac.lib.Appliance;
import it.telecomitalia.ah.hac.lib.ApplianceDescriptor;

/**
 * @author bonino
 *
 */
public class ZigBeeMovementSensorAppliance extends Appliance
{

	private ApplianceDescriptor descriptor;
	
	public ZigBeeMovementSensorAppliance(String pid, Dictionary<?,?> config)
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
