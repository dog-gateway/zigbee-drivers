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

import it.polito.elite.dog.drivers.zigbee.network.info.CmdNotificationInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.domotics.dog2.doglibrary.DogElementDescription;
import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.model.DeviceStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author bonino
 * 
 */
public abstract class ZigBeeDriver
{
	// The appliance managed by the specific implementation of ZigBee device
	// driver
	private ZigBeeApplianceInfo theManagedAppliance;
	
	// a reference to the network driver interface to allow network-level access
	// for sub-classes
	protected ZigBeeNetwork network;
	
	// the DogOnt-defined device managed by instances of classes descending from
	// this abstract class.
	protected ControllableDevice device;
	
	// the state of the device associated to this driver
	protected DeviceStatus currentState;
	
	// the set of notifications associated to the driver
	protected HashMap<String, CmdNotificationInfo> notifications;
	
	// the set of commands associated to the driver
	protected HashMap<String, CmdNotificationInfo> commands;
	
	public ZigBeeDriver(ZigBeeNetwork network, ControllableDevice device)
	{
		// store a reference to the network driver
		this.network = network;
		
		// store a reference to the associate device
		this.device = device;
		
		// fill the data structures depending on the specific device
		// configuration parameters
		this.fillConfiguration();
		
		// call the specific configuration method, if needed
		this.specificConfiguration();
		
		// associate the device-specific driver to the network driver...
		this.addToNetworkDriver(this.theManagedAppliance);
	}
	
	/**
	 * Sets the reference to the {@link ZigBeeApplianceInfo} managed by the
	 * implementing driver...
	 * 
	 * @param appliance
	 *            The {@link ZigBeeApplianceInfo} object managed by the driver
	 *            instance.
	 */
	public void setIAppliance(ZigBeeApplianceInfo appliance)
	{
		// set the appliance
		this.theManagedAppliance = appliance;
	}
	
	/**
	 * Extending classes might implement this method to provide driver-specific
	 * configurations to be done during the driver creation process, before
	 * associating the device-specific driver to the network driver
	 */
	protected abstract void specificConfiguration();
	
	// TODO: newMessageFromHouse (check how to implement)
	
	/**
	 * Abstract method to be implemented by extending classes; performs the
	 * association between the device-specific driver and the underlying network
	 * driver using the appliance data as binding.
	 * 
	 * @param serial
	 */
	protected abstract void addToNetworkDriver(ZigBeeApplianceInfo appliance);
	
	private void fillConfiguration()
	{
		// gets the properties shared by almost all ZigBee devices, i.e. the
		// serial number, etc.
		// specified for the whole device
		Map<String, Set<String>> deviceConfigurationParams = this.device.getDeviceDescriptor()
				.getDevSimpleConfigurationParams();
		
		// check not null
		if (deviceConfigurationParams != null)
		{
			// get the device serial
			Set<String> serialNumbers = deviceConfigurationParams.get(ZigBeeInfo.ZIGBEE_APP_SERIAL);
			if ((serialNumbers != null) && (serialNumbers.size() == 1))
			{
				// store the serial number by initializing the inner
				// ZigBeeApplianceInfo
				this.theManagedAppliance = new ZigBeeApplianceInfo(serialNumbers.iterator().next());
				
			}
		}
		
		// gets the properties associated to each device commmand/notification,
		// if any. E.g.,
		// the unit of measure associated to meter functionalities.
		
		// get parameters associated to each device command (if any)
		Set<DogElementDescription> commandsSpecificParameters = this.device.getDeviceDescriptor()
				.getDevCommandSpecificParams();
		
		// get parameters associated to each device notification (if any)
		Set<DogElementDescription> notificationsSpecificParameters = this.device.getDeviceDescriptor()
				.getDevNotificationSpecificParams();
		
		// --------------- Handle command specific parameters ----------------
		for (DogElementDescription parameter : commandsSpecificParameters)
		{
			
			// the parameter map
			Map<String, String> params = parameter.getElementParams();
			if ((params != null) && (!params.isEmpty()))
			{
				// the name of the command associated to this device...
				String commandName = params.get(ZigBeeInfo.COMMAND_NAME);
				
				if (commandName != null)
					// store the parameters associated to the command
					this.commands.put(commandName, new CmdNotificationInfo(commandName, params));
			}
			
		}
		
		// --------------- Handle notification specific parameters
		// ----------------
		for (DogElementDescription parameter : notificationsSpecificParameters)
		{
			// the parameter map
			Map<String, String> params = parameter.getElementParams();
			if ((params != null) && (!params.isEmpty()))
			{
				// the name of the command associated to this device...
				String notificationName = params.get(ZigBeeInfo.NOTIFICATION_NAME);
				
				if (notificationName != null)
					// store the parameters associated to the command
					this.notifications.put(notificationName, new CmdNotificationInfo(notificationName, params));
			}
			
		}
		
	}
}
