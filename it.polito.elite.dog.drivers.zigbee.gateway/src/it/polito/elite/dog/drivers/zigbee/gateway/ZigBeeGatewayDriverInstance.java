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
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.ZigBeeGateway;
import it.polito.elite.dog.core.library.model.state.NetworkManagementState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.CloseStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OpenStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ApplianceDiscoveryListener;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.dog.drivers.zigbee.network.util.DriverIntersectionData;
import org.energy_home.jemma.ah.hac.IAttributeValue;
import org.energy_home.jemma.ah.hac.lib.ext.IAppliancesProxy;
import org.energy_home.jemma.ah.hac.lib.ext.INetworkManager;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeGatewayDriverInstance extends ZigBeeDriverInstance implements
		ZigBeeGateway, ApplianceDiscoveryListener
{

	// the driver logger
	LogHelper logger;

	// the current list of devices for which dynamic creation can be done
	private Set<ZigBeeDriverInfo> activeDrivers;

	// the appliances proxy used by this gateway driver
	private IAppliancesProxy appliancesProxy;

	// the network manager used by this gateway driver
	private INetworkManager networkManager;

	// the device factory reference
	private DeviceFactory deviceFactory;

	// the device descriptor factory reference
	private DeviceDescriptorFactory descriptorFactory;

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
			Set<ZigBeeDriverInfo> activeDrivers, BundleContext context)
	{
		// call the superclass constructor
		super(network, device);

		// store the device factory reference
		this.deviceFactory = deviceFactory;

		// store the network manager reference
		this.networkManager = networkManager;

		// store the appliances proxy reference
		this.appliancesProxy = appliancesProxy;

		// store the active drivers reference
		this.activeDrivers = activeDrivers;

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

		// add the gateway as new appliance discovery listener
		this.network.addApplianceDiscoveryListener(this);

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
			if (this.networkManager.isNetworkOpen())
			{
				this.updateNetworkManagementState(new NetworkManagementState(
						new OpenStateValue()));
				
				//notify the network state change
				this.notifyOpen();
			}
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
			if (!this.networkManager.isNetworkOpen())
			{
				this.updateNetworkManagementState(new NetworkManagementState(
						new CloseStateValue()));
				
				//notify the network state change
				this.notifyClose();
			}
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR, "Unable to open the network");
		}

	}

	@Override
	public void installAppliance(String deviceId)
	{
		// TODO: extract the appliance pid given the device URI
		this.appliancesProxy.installAppliance(deviceId);
	}

	@Override
	public void deleteAppliance(String deviceId)
	{
		// TODO: extract the appliance pid given the device URI
		this.appliancesProxy.deleteAppliance(deviceId);
	}


	private void updateNetworkManagementState(State newState)
	{
		// update the current state
		this.currentState.setState(
				NetworkManagementState.class.getSimpleName(), newState);

		// debug
		logger.log(LogService.LOG_DEBUG, "Device " + device.getDeviceId()
				+ " is now " + (newState).getCurrentStateValue()[0].getValue());
		
		this.updateStatus();
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

	@Override
	public void applianceDiscovered(ZigBeeApplianceInfo applianceInfo)
	{
		// handle the new appliance
		this.logger.log(
				LogService.LOG_INFO,
				"Detected new appliance with serial: "
						+ applianceInfo.getSerial()
						+ "... trying to find a matching device class");

		// prepare the intersection data
		TreeSet<DriverIntersectionData> matchingDrivers = new TreeSet<DriverIntersectionData>();

		// fill the intersection data
		for (ZigBeeDriverInfo driverInfo : this.activeDrivers)
		{
			// compute the intersection cardinality
			DriverIntersectionData intersectionData = new DriverIntersectionData(
					driverInfo, applianceInfo);

			// debug
			this.logger.log(LogService.LOG_DEBUG,
					"Computing intersection for " + driverInfo.getDriverName()
							+ "(" + driverInfo.getDriverVersion() + "):\n"
							+ intersectionData);
			this.logger.log(LogService.LOG_DEBUG, "Computing intersection for "
					+ driverInfo.getDriverName() + "(basicClusterCardinality: "
					+ driverInfo.getBasicClustersCardinality());

			if (intersectionData.isPerfect())
			{
				// stop and create the driver
				createDevice(intersectionData);
				break;
			}
			else if ((intersectionData.getCardinality()
					- driverInfo.getBasicClustersCardinality()) > 0)
			{
				matchingDrivers.add(intersectionData);
			}
		}

		// check if any intersection has been found
		if (!matchingDrivers.isEmpty())
		{
			// debug
			this.logger.log(LogService.LOG_DEBUG, "Computed ranking: "
					+ matchingDrivers);

			// get the best matching driver
			createDevice(matchingDrivers.last());

		}
		else
		{
			// log the detection failure
			this.logger.log(LogService.LOG_INFO,
					"No match found, device left for future discovery...");
		}

	}

	private void createDevice(DriverIntersectionData intersectionData)
	{
		// build a new device of the driver main class
		String deviceClass = intersectionData.getDriverInfo()
				.getMainDeviceClass();

		// build the device descriptor
		DeviceDescriptor descriptorToAdd = this.buildDeviceDescriptor(
				deviceClass, intersectionData.getApplianceInfo());

		// check not null
		if (descriptorToAdd != null)
		{
			// create the device
			// cross the finger
			this.deviceFactory.addNewDevice(descriptorToAdd);

			// log the new appliance installation
			this.logger.log(LogService.LOG_INFO,
					"New appliance successfully identified...");
		}

	}

	private DeviceDescriptor buildDeviceDescriptor(String deviceClass,
			ZigBeeApplianceInfo appliance)
	{
		// the device descriptor to return
		DeviceDescriptor descriptor = null;

		if (this.descriptorFactory != null)
		{

			// normal workflow...
			if ((deviceClass != null) && (!deviceClass.isEmpty()))
			{
				// create a descriptor definition map
				HashMap<String, Object> descriptorDefinitionData = new HashMap<String, Object>();

				// store the device name
				descriptorDefinitionData.put(DeviceDescriptorFactory.NAME,
						deviceClass + "_" + appliance.getSerial());

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
				descriptorDefinitionData.put("serialNumber",
						appliance.getSerial());

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
				this.logger.log(
						LogService.LOG_INFO,
						"Detected new device: \n\tdeviceUniqueId: "
								+ appliance.getSerial() + "\n\tdeviceClass: "
								+ deviceClass);
			}
		}

		// return
		return descriptor;
	}

	@Override
	public void notifyClose()
	{
		((ZigBeeGateway)this.device).notifyClose();
	}

	@Override
	public void notifyOpen()
	{
		((ZigBeeGateway)this.device).notifyOpen();		
	}

	@Override
	public void updateStatus()
	{
		((Controllable)this.device).updateStatus();
		
	}
}
