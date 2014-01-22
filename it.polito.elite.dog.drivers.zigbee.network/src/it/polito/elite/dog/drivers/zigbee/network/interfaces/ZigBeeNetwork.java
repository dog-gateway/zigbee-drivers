/*
 * Dog 2.0 - ZigBee Network Driver
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
package it.polito.elite.dog.drivers.zigbee.network.interfaces;

import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
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
	public IAppliance addToNetworkDriver(String applianceSerial,
			ZigBeeDriverInstance driver);

	/**
	 * Removes all the association between appliances and the given network
	 * driver
	 * 
	 * @param driver
	 *            the driver to which the appliance handling was delegated
	 */
	public void removeFromNetworkDriver(ZigBeeDriverInstance driver);

	/**
	 * Get the {@link ZigBeeApplianceInfo} object currently associated to the
	 * given serial number, if any, otherwise return null.
	 * 
	 * @param applianceSerial
	 *            The serial number identifying the appliance
	 * @return The corresponding {@link ZigBeeApplianceInfo} object if available
	 *         or null
	 */
	public ZigBeeApplianceInfo getZigBeeApplianceInfo(String applianceSerial);

	/**
	 * Adds a new appliance discovery listener to the set of objects which are
	 * notified of the detection of unknown appliances. Although there should be
	 * only one listener, implemented by the gateway driver, typically, multiple
	 * listeners can be registered.
	 * 
	 * @param listener
	 *            The {@link ApplianceDiscoveryListener} instance to register
	 */
	public void addApplianceDiscoveryListener(
			ApplianceDiscoveryListener listener);

	/**
	 * Removes the given appliance discovery listener from the set of objects
	 * which are notified of the detection of unknown appliances.
	 * 
	 * @param listener
	 *            The {@link ApplianceDiscoveryListener} instance to remove
	 */
	public void removeApplianceDiscoveryListener(
			ApplianceDiscoveryListener listener);
}
