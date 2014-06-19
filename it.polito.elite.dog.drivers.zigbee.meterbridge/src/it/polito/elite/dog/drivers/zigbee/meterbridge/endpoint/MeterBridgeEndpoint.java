/*
 * Dog 2.0 - ZigBee MeterBridge
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
package it.polito.elite.dog.drivers.zigbee.meterbridge.endpoint;

import it.polito.elite.dog.drivers.zigbee.meterbridge.appliance.BridgedMeteringAppliance;
import org.energy_home.jemma.ah.hac.ApplianceException;
import org.energy_home.jemma.ah.hac.lib.EndPoint;


/**
 * A custom {@link EndPoint} bound to the given appliance.
 * @author bonino
 *
 */
public class MeterBridgeEndpoint extends EndPoint
{

	public MeterBridgeEndpoint(String type, BridgedMeteringAppliance appliance) throws ApplianceException
	{
		super(type);
		this.appliance = appliance;
	}

}
