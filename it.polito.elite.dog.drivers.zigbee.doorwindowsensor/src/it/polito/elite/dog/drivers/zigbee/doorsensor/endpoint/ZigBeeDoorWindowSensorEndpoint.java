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
package it.polito.elite.dog.drivers.zigbee.doorsensor.endpoint;

import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.lib.Appliance;
import it.telecomitalia.ah.hac.lib.EndPoint;

/**
 * @author bonino
 *
 */
public class ZigBeeDoorWindowSensorEndpoint extends EndPoint
{

	/**
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