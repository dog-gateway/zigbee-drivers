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
	private HashMap<IAppliance, IApplicationEndPoint> applianceToEndpoint;
	
	/**
	 * 
	 */
	public ZigBeeNetworkDriver()
	{
		// init data structures
		this.applianceToEndpoint = new HashMap<IAppliance, IApplicationEndPoint>();
	}
	
	public void activate(BundleContext context)
	{
		this.logger = new DogLogInstance(context);
		
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Activated...");
	}
	
	public void deactivate()
	{
		
	}
	
	@Override
	public IServiceCluster[] getServiceClusters()
	{
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Get Service clusters");
		return null;
	}
	
	@Override
	public void notifyApplianceAdded(IApplicationEndPoint endPoint, IAppliance appliance)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance added: "
				+ appliance.getDescriptor().getFriendlyName() + " Pid: " + appliance.getPid());
		
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance endpoints: ");
		
		for (String endpointType : appliance.getEndPointTypes())
		{
			this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Endpoint type: " + endpointType);
		}
		
		// store the couple appliance endpoint
		applianceToEndpoint.put(appliance, endPoint);
		
		this.checkCommands(appliance);
		
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Application endpoint: ");
		
		for (String clusterName : endPoint.getServiceClusterNames())
		{
			this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "ClusterName: " + clusterName);
		}
	}
	
	@Override
	public void notifyApplianceRemoved(IAppliance appliance)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance removed: "
				+ appliance.getDescriptor().getFriendlyName());
		
	}
	
	@Override
	public void notifyApplianceAvailabilityUpdated(IAppliance appliance)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance updated: "
				+ appliance.getDescriptor().getFriendlyName());
		
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Appliance updated endpoints: ");
		
		this.checkCommands(appliance);
		
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
					onOff.execToggle(this.applianceToEndpoint.get(appliance).getDefaultRequestContext());
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
					int measure = meter.getIstantaneousDemand(this.applianceToEndpoint.get(appliance)
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
	
	@Override
	public IAppliance addToNetworkDriver(String applianceSerial, ZigBeeDriver driver)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IAppliance getIAppliance(String applianceSerial)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + "Updated configuration...");
		
		// get the bundle configuration parameters
		if (properties != null)
		{
			// try to get the baseline polling time
			String pollingTimeAsString = (String) properties.get("pollingTimeMillis");
			
			// trim leading and trailing spaces
			pollingTimeAsString = pollingTimeAsString.trim();
			
			// check not null
			if (pollingTimeAsString != null)
			{
				// parse the string
				this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId + pollingTimeAsString);
			}
		}
		
	}
	
}
