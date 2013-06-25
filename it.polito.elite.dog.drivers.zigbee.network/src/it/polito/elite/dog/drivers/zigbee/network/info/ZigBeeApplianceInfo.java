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
	// the IAppliance object representing the real ZigBee appliance for which
	// this info object is created
	private IAppliance appliance;
	
	// the application endpoint associated to the real ZigBee appliance for
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
		// store the IAppliance object associated to this info object
		this.appliance = appliance;
		
		// store the IApplicationEndpoint object associated to this info object
		this.endpoint = endpoint;
		
		// store the appliance serial
		this.serial = ZigBeeApplianceInfo.extractApplianceSerial(appliance);
	}
	
	/**
	 * Get the {@IAppliance} instance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @return the appliance
	 */
	public IAppliance getAppliance()
	{
		return appliance;
	}
	
	/**
	 * Set the {@IAppliance} instance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @param appliance
	 *            the appliance to set
	 */
	public void setAppliance(IAppliance appliance)
	{
		this.appliance = appliance;
	}
	
	/**
	 * Gets the {@link IApplicationEndpoint} instance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @return the endpoint
	 */
	public IApplicationEndPoint getEndpoint()
	{
		return endpoint;
	}
	
	/**
	 * Sets the {@link IApplicationEndpoint} instance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @param endpoint
	 *            the endpoint to set
	 */
	public void setEndpoint(IApplicationEndPoint endpoint)
	{
		this.endpoint = endpoint;
	}
	
	/**
	 * Gets the serial number of the appliance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @return the serial
	 */
	public String getSerial()
	{
		return serial;
	}
	
	/**
	 * Sets the serial number of the appliance associated to the device
	 * described by this ZigBeeApplianceInfo instance.
	 * 
	 * @param serial
	 *            the serial to set
	 */
	public void setSerial(String serial)
	{
		this.serial = serial;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		// the string buffer for holding the appliance description
		StringBuffer applianceAsString = new StringBuffer();
		
		// the appliance friendly name
		applianceAsString.append("Appliance fridely name: " + this.appliance.getDescriptor().getFriendlyName() + "\n");
		applianceAsString.append("Appliance PID: " + this.appliance.getPid() + "\n");
		applianceAsString.append("Appliance endpoints:\n");
		
		for (String endpointType : this.appliance.getEndPointTypes())
		{
			applianceAsString.append("\tEndpoint type: " + endpointType + "\n");
		}
		
		// add IApplicationEndpoint details
		applianceAsString.append("Application endpoint:\n");
		
		for (String clusterName : this.endpoint.getServiceClusterNames())
		{
			applianceAsString.append("\tClusterName: " + clusterName + "\n");
		}
		
		// return the string rendering of this appliance info object
		return applianceAsString.toString();
	}
	
	/**
	 * Utility method for extracting the serial number of an appliance from the
	 * IAppliance instance representing the ZigBee device
	 * 
	 * @param appliance
	 * @return the serial number of the appliance
	 */
	public static String extractApplianceSerial(IAppliance appliance)
	{
		// extract the device serial number (this might be frail...)
		String appliancePid = appliance.getPid();
		return appliancePid.substring(appliancePid.lastIndexOf('.')+1);
	}
}
