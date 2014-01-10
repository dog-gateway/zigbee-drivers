/*
 * Dog - ZigBee Gateway Driver
 * 
 * 
 * Copyright 2014 Dario Bonino 
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
package it.polito.elite.dog.drivers.zigbee.gateway;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptor;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.HomeGateway;
import it.polito.elite.dog.core.library.model.devicecategory.ZigBeeGateway;
import it.polito.elite.dog.core.library.model.state.NetworkManagementState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.CloseStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OpenStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.hac.IAttributeValue;
import it.telecomitalia.ah.hac.lib.ext.IAppliancesProxy;
import it.telecomitalia.ah.hac.lib.ext.INetworkManager;
import it.telecomitalia.ah.hac.IAppliance;

import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeGatewayDriverInstance extends ZigBeeDriver implements
		ZigBeeGateway
{

	// the driver logger
	LogHelper logger;

	// the current list of devices for which dynamic creation can be done
	private ConcurrentHashMap<String, String> supportedDevices;

	// the appliances proxy used by this gateway driver
	private IAppliancesProxy appliancesProxy;

	// the network manager used by this gateway driver
	private INetworkManager networkManager;

	// the device factory reference
	private DeviceFactory deviceFactory;

	// the device descriptor factory reference
	private DeviceDescriptorFactory descriptorFactory;

	// the time to wait before attempting automatic device detection
	private long waitBeforeDeviceInstall = 0;

	/**
	 * 
	 * @param zigBeeNetwork
	 * @param networkManager
	 * @param appliancesProxy
	 * @param deviceFactory
	 * @param device
	 * @param context
	 */
	public ZigBeeGatewayDriverInstance(ZigBeeNetwork network,
			DeviceFactory deviceFactory, IAppliancesProxy appliancesProxy,
			INetworkManager networkManager, ControllableDevice device,
			BundleContext context)
	{
		// call the superclass constructor
		super(network, device);

		// store the device factory reference
		this.deviceFactory = deviceFactory;
		
		// store the network manager reference
		this.networkManager = networkManager;
		
		// store the appliances proxy reference
		this.appliancesProxy = appliancesProxy;

		// create a logger
		logger = new LogHelper(context);

		// create the device descriptor factory
		try
		{
			this.descriptorFactory = new DeviceDescriptorFactory(context
					.getBundle().getEntry("/deviceTemplates"));
		}
		catch (Exception e)
		{

			this.logger.log(LogService.LOG_ERROR,
					"Error while creating DeviceDescriptorFactory ", e);
		}

		// create a new device state (according to the current DogOnt model, no
		// state is actually associated to a Modbus gateway)
		currentState = new DeviceStatus(device.getDeviceId());

		// initialize device states
		this.initializeStates();

	}

	/**
	 * @return the supportedDevices
	 */
	public ConcurrentHashMap<String, String> getSupportedDevices()
	{
		return supportedDevices;
	}

	/**
	 * @param supportedDevices
	 *            the supportedDevices to set
	 */
	public void setSupportedDevices(
			ConcurrentHashMap<String, String> supportedDevices)
	{
		// simplest updated policy : replacement
		this.supportedDevices = supportedDevices;

		// debug
		this.logger.log(LogService.LOG_DEBUG,
				"Updated dynamic device creation db");
	}

	// TODO: borrowed from ZWave rewrite to address Zigbee device identification
	private DeviceDescriptor buildDeviceDescriptor()
	{
		// the device descriptor to return
		DeviceDescriptor descriptor = null;

		if (this.descriptorFactory != null)
		{

			// get the new device data

			// get the manufacturer id
			String manufacturerId = "";
			String manufacturerProductType = "";
			String manufacturerProductId = "";

			// wait for instances to be read.... (may be read with a certain
			// variable delay)
			try
			{
				Thread.sleep(this.waitBeforeDeviceInstall);
			}
			catch (InterruptedException e1)
			{
				this.logger
						.log(LogService.LOG_WARNING,
								"Instance wait time was less than necessary due to interrupted thread, device instantiation might not be accurate.",
								e1);
			}

			// build the 4th id (number of instances)
			int numberOfInstances = 1;

			// build the device unique id
			String extendedDeviceUniqueId = manufacturerId + "-"
					+ manufacturerProductType + "-" + manufacturerProductId
					+ "-" + numberOfInstances;

			// build the device unique id
			String deviceUniqueId = manufacturerId + "-"
					+ manufacturerProductType + "-" + manufacturerProductId;

			// get the device class
			String deviceClass = this.supportedDevices
					.get(extendedDeviceUniqueId);

			// check if not extended
			if (deviceClass == null)
				deviceClass = this.supportedDevices.get(deviceUniqueId);

			// normal workflow...
			if ((deviceClass != null) && (!deviceClass.isEmpty()))
			{
				// create a descriptor definition map
				HashMap<String, Object> descriptorDefinitionData = new HashMap<String, Object>();

				// store the device name
				descriptorDefinitionData.put(DeviceDescriptorFactory.NAME,
						deviceClass + "_" + 1);

				// store the device description
				descriptorDefinitionData.put(
						DeviceDescriptorFactory.DESCRIPTION,
						"New Device of type " + deviceClass);

				// store the device gateway
				descriptorDefinitionData.put(DeviceDescriptorFactory.GATEWAY,
						this.device.getDeviceId());

				// store the device location
				descriptorDefinitionData.put(DeviceDescriptorFactory.LOCATION,
						"");

				// store the node id
				descriptorDefinitionData.put("nodeId", "" + 1);

				// get the device descriptor
				try
				{
					descriptor = this.descriptorFactory.getDescriptor(
							descriptorDefinitionData, deviceClass);
				}
				catch (Exception e)
				{
					this.logger
							.log(LogService.LOG_ERROR,
									"Error while creating DeviceDescriptor for the just added device ",
									e);
				}

				// debug dump
				this.logger.log(LogService.LOG_INFO,
						"Detected new device: \n\tdeviceUniqueId: "
								+ deviceUniqueId + "\n\tdeviceClass: "
								+ deviceClass);
			}
		}

		// return
		return descriptor;
	}

	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}

	@Override
	public void openNetwork()
	{
		// open the network
		try
		{
			this.networkManager.openNetwork();
			if(this.networkManager.isNetworkOpen())
				this.notifyStateChanged(new NetworkManagementState(new OpenStateValue()));
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR, "Unable to open the network");
		}

	}
	
	@Override
	public void closeNetwork()
	{
		// close the network
		try
		{
			this.networkManager.closeNetwork();
			if(!this.networkManager.isNetworkOpen())
				this.notifyStateChanged(new NetworkManagementState(new CloseStateValue()));
			
			//TODO: handle device discovery
			this.discoverNewDevices();
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR, "Unable to open the network");
		}
		
	}

	@Override
	public void installAppliance(String deviceId)
	{
		//TODO: extract the appliance pid given the device URI
		this.appliancesProxy.installAppliance(deviceId);		
	}

	@Override
	public void deleteAppliance(String deviceId)
	{
		//TODO: extract the appliance pid given the device URI
		this.appliancesProxy.deleteAppliance(deviceId);		
	}

	

	@Override
	public void notifyStateChanged(State newState)
	{
		// update the current state
		this.currentState.setState(
				NetworkManagementState.class.getSimpleName(), newState);

		// debug
		logger.log(LogService.LOG_DEBUG, "Device " + device.getDeviceId()
				+ " is now " + (newState).getCurrentStateValue()[0].getValue());

		// call the super method
		((HomeGateway) device).notifyStateChanged(newState);

	}

	@Override
	protected void specificConfiguration()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void addToNetworkDriver(ZigBeeApplianceInfo appliance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void newMessageFromHouse(Integer endPointId, String clusterName,
			String attributeName, IAttributeValue attributeValue)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Initializes the state asynchronously as required by OSGi
	 */
	private void initializeStates()
	{
		// initialize the state
		if (this.networkManager.isNetworkOpen())
		{
			this.currentState.setState(
					NetworkManagementState.class.getSimpleName(),
					new NetworkManagementState(new OpenStateValue()));
		}
		else
		{
			this.currentState.setState(
					NetworkManagementState.class.getSimpleName(),
					new NetworkManagementState(new CloseStateValue()));
		}

	}

	/**
	 * 
	 */
	private void discoverNewDevices()
	{
		//get the list of installed appliances...
		@SuppressWarnings("unchecked")
		List<IAppliance> appliances = (List<IAppliance>)this.appliancesProxy.getAppliances();
		
		//iterate over appliances
		for(IAppliance currentAppliance : appliances)
		{
			//get the appliance serial
			String currentApplianceSerial = ZigBeeApplianceInfo.extractApplianceSerial(currentAppliance.getPid());
			
			//check against the set of managed appliances
			ZigBeeApplianceInfo currentApplianceInfo = this.network.getZigBeeApplianceInfo(currentApplianceSerial);
			
			//if null the appliance is still not configured
			if(currentApplianceInfo==null)
			{
				//handle the new appliance
				this.logger.log(LogService.LOG_INFO, "Detected new appliance with serial: "+currentApplianceSerial);
				
				//try to get the appliance information
				currentAppliance.getDescriptor().getDeviceType();
			}
		}
	}
}
