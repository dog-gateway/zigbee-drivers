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
package it.polito.elite.dog.drivers.zigbee.doorwindowsensor.endpoint;

import org.energy_home.jemma.ah.hac.ApplianceException;
import org.energy_home.jemma.ah.hac.lib.Appliance;
import org.energy_home.jemma.ah.hac.lib.EndPoint;

/**
 * A custom {@link EndPoint} retaining a pointer to the appliance to which
 * belongs.
 * 
 * @author bonino
 * 
 */
public class ZigBeeDoorWindowSensorEndpoint extends EndPoint
{

	/**
	 * Class constructor, takes the appliance to which the cluster belongs as
	 * parameter.
	 * 
	 * @param type
	 * @throws ApplianceException
	 */
	public ZigBeeDoorWindowSensorEndpoint(String type, Appliance appliance)
			throws ApplianceException
	{
		super(type);
		this.appliance = appliance;
	}

}
