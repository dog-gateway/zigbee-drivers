/*
 * Dog 2.0 - ZigBee Network Driver
 * 
 * Copyright [Jun 19, 2013] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zigbee.network.interfaces;

import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver;
import it.telecomitalia.ah.hac.IAppliance;

/**
 * @author bonino
 * 
 * @since June, 2013
 */
public interface ZigBeeNetwork
{
	/**
	 * Adds the given ZigBee driver as "delegate" for handling commands and
	 * notifications to/from the appliance having the given serial number.
	 * 
	 * @param applianceSerial
	 *            the appliance serial number (which is supposed to be unique)
	 * @param driver
	 *            the driver to which the appliance handling is delegated
	 * @return a pointer to the IAppliance object representing the appliance (if
	 *         active on the network, if null then the device-specific driver
	 *         will need to call the getApplianceMethod at each requested
	 *         command, unless a later discovery sets the appliance associated
	 *         to the driver by calling the driver's setIAppliance method.
	 */
	public IAppliance addToNetworkDriver(String applianceSerial, ZigBeeDriver driver);
	
	/**
	 * Get the IAppliance object currently associated to the given serial
	 * number, if any, otherwise return null.
	 * 
	 * @param applianceSerial The serial number identifying the appliance
	 * @return The corresponding IAppliance object if available or null
	 */
	public IAppliance getIAppliance(String applianceSerial);
}
