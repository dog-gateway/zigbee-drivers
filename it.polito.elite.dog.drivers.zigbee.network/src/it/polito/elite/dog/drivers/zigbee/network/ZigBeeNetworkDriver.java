/*
 * Dog 2.0 - ZigBee Network Driver
 * 
 * Copyright [2013] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zigbee.network;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.cluster.zigbee.metering.SimpleMeteringServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IAppliance;
import it.telecomitalia.ah.hac.IApplicationEndPoint;
import it.telecomitalia.ah.hac.IApplicationService;
import it.telecomitalia.ah.hac.IEndPoint;
import it.telecomitalia.ah.hac.IServiceCluster;
import it.telecomitalia.ah.hac.ServiceClusterException;

import java.util.Dictionary;
import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

/**
 * The network driver for devices based on the ZigBee network protocol
 * 
 * @author bonino
 * 
 */
public class ZigBeeNetworkDriver implements IApplicationService, ZigBeeNetwork, ManagedService
{
	// the bundle logger
	private LogService logger;
	
	// the log id
	private static final String logId = "[ZigBeeNetworkDriver]: ";
	
	// the appliance / endpoint map
	private HashMap<String, ZigBeeApplianceInfo> connectedAppliances;
	
	// the serial/driver map
	private HashMap<String, ZigBeeDriver> connectedDrivers;
	
	/**
	 * Creates an instance of {@link ZigBeeNetworkDriver}, typically called
	 * before activation.
	 */
	public ZigBeeNetworkDriver()
	{
		// init data structures
		this.connectedAppliances = new HashMap<String, ZigBeeApplianceInfo>();
		this.connectedDrivers = new HashMap<String, ZigBeeDriver>();
	}
	
	public void activate(BundleContext context)
	{
		// initialize the class logger...
		this.logger = new DogLogInstance(context);
		
		// debug: signal activation...
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Activated...");
	}
	
	public void deactivate()
	{
		// TODO: react to deactivation...
	}
	
	@Override
	public IServiceCluster[] getServiceClusters()
	{
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Get Service clusters");
		return null;
	}
	
	@Override
	public void notifyApplianceAdded(IApplicationEndPoint endpoint, IAppliance appliance)
	{
		
		// create a ZigBeeApplianceInfo object representing the appliance
		ZigBeeApplianceInfo applianceInfo = new ZigBeeApplianceInfo(endpoint, appliance);
		
		// store the appliance info
		this.connectedAppliances.put(applianceInfo.getSerial(), applianceInfo);
		
		// update any already connected drivers
		this.updateApplianceDriverBinding(applianceInfo.getSerial());
		
		// debug
		this.logger.log(LogService.LOG_INFO, ZigBeeNetworkDriver.logId + "Added appliance: \n" + applianceInfo);
		
		// trial, TODO: remove this upon onoff driver completion
		this.checkCommands(appliance);
		
	}
	
	@Override
	public void notifyApplianceRemoved(IAppliance appliance)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance removed: "
				+ appliance.getDescriptor().getFriendlyName());
		
		//TODO: update bound drivers (e.g., by removing the bound appliance info)
		
	}
	
	@Override
	public void notifyApplianceAvailabilityUpdated(IAppliance appliance)
	{
		// get the appliance serial number
		String serial = ZigBeeApplianceInfo.extractApplianceSerial(appliance);
		
		// get the appliance from the set of connected appliances
		ZigBeeApplianceInfo applianceInfo = this.connectedAppliances.get(serial);
		
		// check not null
		if (applianceInfo != null)
		{
			// update the appliance reference...
			applianceInfo.setAppliance(appliance);
			
			// debug
			this.logger.log(LogService.LOG_INFO, ZigBeeNetworkDriver.logId + "Updated appliance: \n" + applianceInfo);
		}
		
		// trial, TODO: remove this upon onoff driver completion
		this.checkCommands(appliance);
		
	}
	
	@Override
	public IAppliance addToNetworkDriver(String applianceSerial, ZigBeeDriver driver)
	{
		//add to the map of connected drivers
		this.connectedDrivers.put(applianceSerial, driver);
		
		//check if the appliance already exists and if such, call back the driver method...
		this.updateApplianceDriverBinding(applianceSerial);
		
		return null;
	}
	
	private void checkCommands(IAppliance appliance)
	{
		for (IEndPoint endpoint : appliance.getEndPoints())
		{
			this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Endpoint type: " + endpoint.getType());
			
			// check endpoint
			if (endpoint.getType().equals("ah.ep.zigbee.LoadControlDevice"))
			{
				OnOffServer onOff = (OnOffServer) endpoint.getServiceCluster(OnOffServer.class.getName());
				
				try
				{
					onOff.execToggle(this.connectedAppliances
							.get(ZigBeeApplianceInfo.extractApplianceSerial(appliance)).getEndpoint()
							.getDefaultRequestContext());
					this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Toggled!");
				}
				catch (ApplianceException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ServiceClusterException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (endpoint.getType().equals("ah.ep.zigbee.MeteringDevice"))
			{
				SimpleMeteringServer meter = (SimpleMeteringServer) endpoint
						.getServiceCluster(SimpleMeteringServer.class.getName());
				
				try
				{
					int measure = meter.getIstantaneousDemand(this.connectedAppliances
							.get(ZigBeeApplianceInfo.extractApplianceSerial(appliance)).getEndpoint()
							.getDefaultRequestContext());
					this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Measure: " + measure);
				}
				catch (ApplianceException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ServiceClusterException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Updates the existing bindings between appliance and drivers
	 * @param applianceSerial
	 */
	private void updateApplianceDriverBinding(String applianceSerial)
	{
		//get the associated appliance info, if any
		ZigBeeApplianceInfo applianceInfo = this.connectedAppliances.get(applianceSerial);
		
		//get the associated driver, if any
		ZigBeeDriver driver = this.connectedDrivers.get(applianceSerial);
		
		//notify the driver if an appliance is available with the given serial number
		if((applianceInfo!=null)&&(driver!=null))
		{
			driver.setIAppliance(applianceInfo);
		}
		
	}

	@Override
	public ZigBeeApplianceInfo getZigBeeApplianceInfo(String applianceSerial)
	{
		return this.connectedAppliances.get(applianceSerial);
	}
	
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Updated configuration...");
		
		// left for future uses...
	}
	
}
