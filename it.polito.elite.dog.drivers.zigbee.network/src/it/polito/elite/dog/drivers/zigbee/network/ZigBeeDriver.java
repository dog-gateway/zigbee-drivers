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
package it.polito.elite.dog.drivers.zigbee.network;

import it.telecomitalia.ah.hac.IAppliance;

/**
 * @author bonino
 * 
 */
public abstract class ZigBeeDriver
{
	// The appliance managed by the specific implementation of ZigBee device
	// driver
	private IAppliance theManagedAppliance;
	
	/**
	 * Sets the reference to the object implementing the {@link IAppliance}
	 * interface managed by the implementing driver...
	 * 
	 * @param appliance
	 *            The IAppliance object managed by the driver instance.
	 */
	public void setIAppliance(IAppliance appliance)
	{
		// set the appliance
		this.theManagedAppliance = appliance;
	}
}
