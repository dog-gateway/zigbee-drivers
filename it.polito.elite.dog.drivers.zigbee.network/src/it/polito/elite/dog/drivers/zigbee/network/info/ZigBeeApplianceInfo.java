/*
 * Dog 2.0 - ZigBee Network Driver
 * 
 * Copyright [Jun 20, 2013] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zigbee.network.info;

import it.telecomitalia.ah.hac.IAppliance;
import it.telecomitalia.ah.hac.IApplicationEndPoint;

/**
 * A class used to store information about zigbee appliances, in particular the
 * associated IAppliance, if any, the serial number, the IAppliance endpoint and
 * the configuration parameters
 * 
 * @author bonino
 * 
 */
public class ZigBeeApplianceInfo
{
	// the IAppliance object rappresenting the real zigbee appliance for which
	// this info object is created
	private IAppliance appliance;
	
	// the application endpoint associated to the real zigbee appliance for
	// which
	// this info object is created
	private IApplicationEndPoint endpoint;
	
	// the appliance serial number
	private String serial;
	
	/**
	 * The class constructor, allows for an initial empty instantiation with
	 * only the appiance serial specified
	 */
	public ZigBeeApplianceInfo(String applianceSerial)
	{
		// store the appliance serial
		this.serial = applianceSerial;
	}
	
	public ZigBeeApplianceInfo(IApplicationEndPoint endpoint, IAppliance appliance)
	{
		//store the IAppliance object associated to this info object
		this.appliance = appliance;
		
		//store the IApplicationEndpoint object associated to this info object
		this.endpoint = endpoint;
		
		//extract the device serial number
		String appliancePid = this.appliance.getPid();
		this.serial = appliancePid.substring(appliancePid.lastIndexOf('.'));
	}
}
