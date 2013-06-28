/*
 * Dog 2.0 - ZigBee OnOffDevice Driver
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
package it.polito.elite.dog.drivers.zigbee.onoffdevice;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.polito.elite.domotics.model.DeviceStatus;
import it.polito.elite.domotics.model.devicecategory.Buzzer;
import it.polito.elite.domotics.model.devicecategory.ElectricalSystem;
import it.polito.elite.domotics.model.devicecategory.Lamp;
import it.polito.elite.domotics.model.devicecategory.MainsPowerOutlet;
import it.polito.elite.domotics.model.devicecategory.SimpleLamp;
import it.polito.elite.domotics.model.state.OnOffState;
import it.polito.elite.domotics.model.state.State;
import it.polito.elite.domotics.model.statevalue.OffStateValue;
import it.polito.elite.domotics.model.statevalue.OnStateValue;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IAttributeValue;
import it.telecomitalia.ah.hac.IEndPoint;
import it.telecomitalia.ah.hac.IServiceCluster;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.SubscriptionParameters;

/**
 * @author bonino
 * 
 */
public class ZigBeeOnOffDeviceDriverInstance extends ZigBeeDriver implements Lamp, SimpleLamp, Buzzer, MainsPowerOutlet
{
	// the class logger
	private LogService logger;
	
	// the on-off cluster associated to the appliance managed by this driver
	private OnOffServer onOffClusterServer;
	
	/**
	 * Creates an instance of device-specific driver associated to a given
	 * device and using a given network driver
	 * 
	 * @param network
	 *            the {@link ZigBeeNetwork} driver used by this instance
	 * @param device
	 *            the {@link ControllableDevice} handled by this instance
	 * @param context
	 *            the {@link BundleContext} of the driver bundle managing this
	 *            instance.
	 */
	public ZigBeeOnOffDeviceDriverInstance(ZigBeeNetwork network, ControllableDevice device, BundleContext context)
	{
		super(network, device);
		
		// create a logger
		this.logger = new DogLogInstance(context);
		
		// read the initial state of devices
		this.initializeStates();
	}
	
	@Override
	public void storeScene(Integer sceneNumber)
	{
		// intentionally left empty
		
	}
	
	@Override
	public void deleteScene(Integer sceneNumber)
	{
		// intentionally left empty
		
	}
	
	@Override
	public void deleteGroup(String groupID)
	{
		// intentionally left empty
		
	}
	
	@Override
	public void storeGroup(String groupID)
	{
		// intentionally left empty
		
	}
	
	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}
	
	@Override
	public void on()
	{
		// check if the appliance is already available
		if ((this.theManagedAppliance != null) && (this.onOffClusterServer != null))
		{
			try
			{
				// send the on command to the corresponding cluster
				this.onOffClusterServer.execOn(this.theManagedAppliance.getEndpoint().getDefaultRequestContext());
				
				// temporary shor-circuit for state update
				// this.changeCurrentState(OnOffState.ON);
				
				// log the command
				this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId
						+ " Sent OnCommand to the ZigBee device with serial " + this.theManagedAppliance.getSerial());
			}
			catch (ApplianceException e)
			{
				this.logger.log(LogService.LOG_ERROR, ZigBeeOnOffDeviceDriver.logId
						+ "Error (ApplianceException) while sending OnCommand to the ZigBee appliance with serial: "
						+ this.theManagedAppliance.getSerial(), e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeOnOffDeviceDriver.logId
										+ "Error (ServiceClusterException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(), e);
			}
		}
	}
	
	@Override
	public void off()
	{
		// check if the appliance is already available
		if ((this.theManagedAppliance != null) && (this.onOffClusterServer != null))
		{
			try
			{
				// send the off command to the corresponding cluster
				this.onOffClusterServer.execOff(this.theManagedAppliance.getEndpoint().getDefaultRequestContext());
				
				// temporary shor-circuit for state update
				// this.changeCurrentState(OnOffState.OFF);
				
				// log the command
				this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId
						+ " Sent OffCommand to the ZigBee device with serial " + this.theManagedAppliance.getSerial());
			}
			catch (ApplianceException e)
			{
				this.logger.log(LogService.LOG_ERROR, ZigBeeOnOffDeviceDriver.logId
						+ "Error (ApplianceException) while sending OnCommand to the ZigBee appliance with serial: "
						+ this.theManagedAppliance.getSerial(), e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeOnOffDeviceDriver.logId
										+ "Error (ServiceClusterException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(), e);
			}
		}
		
	}
	
	@Override
	public void notifyStateChanged(State newState)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + "Device " + this.device.getDeviceId()
				+ " is now " + ((OnOffState) newState).getCurrentStateValue()[0].getValue());
		((ElectricalSystem) this.device).notifyStateChanged(newState);
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver#setIAppliance
	 * (it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo)
	 */
	@Override
	public void setIAppliance(ZigBeeApplianceInfo appliance)
	{
		// call the superclass method
		super.setIAppliance(appliance);
		
		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();
		
		for (IEndPoint endpoint : endpoints)
		{
			// get the OnOff cluster
			IServiceCluster cluster = endpoint.getServiceCluster(OnOffServer.class.getName());
			
			if (cluster != null)
			{
				// store the cluster
				this.onOffClusterServer = (OnOffServer) cluster;
				
				// set the attribute subscription
				try
				{
					cluster.setAttributeSubscription(OnOffServer.ATTR_OnOff_NAME, new SubscriptionParameters(0, 5000,
							0), this.theManagedAppliance.getEndpoint().getDefaultRequestContext());
				}
				catch (ApplianceException e)
				{
					this.logger
							.log(LogService.LOG_ERROR,
									ZigBeeOnOffDeviceDriver.logId
											+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
											+ this.theManagedAppliance.getSerial(), e);
				}
				catch (ServiceClusterException e)
				{
					this.logger
							.log(LogService.LOG_ERROR,
									ZigBeeOnOffDeviceDriver.logId
											+ "Error (ServiceClusterException) while setting subscription to the ZigBee appliance with serial: "
											+ this.theManagedAppliance.getSerial(), e);
				}
				
				// stop the iteration as the needed cluster has been found
				break;
			}
		}
	}
	
	@Override
	protected void specificConfiguration()
	{
		// prepare the device state map
		this.currentState = new DeviceStatus(device.getDeviceId());
	}
	
	@Override
	protected void addToNetworkDriver(ZigBeeApplianceInfo appliance)
	{
		// add this driver instance to the network driver
		this.network.addToNetworkDriver(appliance.getSerial(), this);
	}
	
	@Override
	protected void newMessageFromHouse(Integer endPointId, String clusterName, String attributeName,
			IAttributeValue attributeValue)
	{
		// handle OnOffCluster only...
		if ((clusterName.equals(OnOffServer.class.getName())) && (attributeName.equals(OnOffServer.ATTR_OnOff_NAME)))
		{
			// translate true and false to on and off
			boolean on = (Boolean) attributeValue.getValue();
			
			if (on)
				this.changeCurrentState(OnOffState.ON);
			else
				this.changeCurrentState(OnOffState.OFF);
		}
		
	}
	
	/**
	 * Initialize the device state...
	 */
	private void initializeStates()
	{
		// set the initial state at off
		this.currentState.setState(OnOffState.class.getSimpleName(), new OnOffState(new OffStateValue()));
	}
	
	/**
	 * Check if the current state has been changed. In that case, fire a state
	 * change message, otherwise it does nothing
	 * 
	 * @param OnOffValue
	 *            OnOffState.ON or OnOffState.OFF
	 */
	private void changeCurrentState(String onOffValue)
	{
		String currentStateValue = (String) this.currentState.getState(OnOffState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + "Received state :" + onOffValue);
		
		// if the current states it is different from the new state
		if (!currentStateValue.equalsIgnoreCase(onOffValue))
		{
			State newState;
			// set the new state to on or off...
			if (onOffValue.equalsIgnoreCase(OnOffState.ON))
			{
				newState = new OnOffState(new OnStateValue());
			}
			else
			{
				newState = new OnOffState(new OffStateValue());
			}
			// ... then set the new state for the device and throw a state
			// changed notification
			this.currentState.setState(newState.getStateName(), newState);
			
			this.notifyStateChanged(newState);
		}
		
	}
	
}
