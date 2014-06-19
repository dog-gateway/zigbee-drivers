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
package it.polito.elite.dog.drivers.zigbee.doorwindowsensor.appliance;

import java.util.Dictionary;

import it.polito.elite.dog.drivers.zigbee.doorwindowsensor.ZigBeeDoorWindowSensorDriverInstance;
import org.energy_home.jemma.ah.hac.ApplianceException;
import org.energy_home.jemma.ah.hac.IApplianceDescriptor;
import org.energy_home.jemma.ah.hac.lib.Appliance;
import org.energy_home.jemma.ah.hac.lib.ApplianceDescriptor;

/**
 * A simple virtual Appliance class which exports and OnOffServer cluster
 * internally linked to a {@link ZigBeeDoorWindowSensorDriverInstance}. Cluster
 * commands are bound to the instance state change primitives allowing to map on
 * commands to open notifications and off commands to close notifications.
 * 
 * @author bonino
 * 
 */
public class ZigBeeDoorWindowSensorAppliance extends Appliance
{

	// the appliance descriptor, used to set a unique appliance id that can be
	// traced back to Dog
	private ApplianceDescriptor descriptor;

	/**
	 * Constructor, merely adds the appliance pid to the default appliance constructor
	 * @param pid
	 * @param config
	 * @throws ApplianceException
	 */
	public ZigBeeDoorWindowSensorAppliance(String pid, Dictionary<?, ?> config)
			throws ApplianceException
	{
		super(pid, config);

		//set the appliance descriptor
		this.descriptor = new ApplianceDescriptor(
				"it.polito.elite.drivers.zigbee", pid);
	}

	@Override
	public IApplianceDescriptor getDescriptor()
	{
		//provide back the appliance descriptor
		return this.descriptor;
	}

}
