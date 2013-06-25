/**
 * 
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
import it.telecomitalia.ah.hac.IEndPoint;
import it.telecomitalia.ah.hac.ServiceClusterException;

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
				this.changeCurrentState(OnOffState.ON);
				
				//log the command
				this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId+" Sent OnCommand to the ZigBee device with serial "+this.theManagedAppliance.getSerial());
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
				this.onOffClusterServer.execOff(this.theManagedAppliance.getEndpoint().getDefaultRequestContext(true));
				
				// temporary shor-circuit for state update
				this.changeCurrentState(OnOffState.OFF);
				
				//log the command
				this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId+" Sent OffCommand to the ZigBee device with serial "+this.theManagedAppliance.getSerial());
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
		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();
		
		for (IEndPoint endpoint : endpoints)
		{
			// get the OnOff cluster
			OnOffServer cluster = (OnOffServer) endpoint.getServiceCluster(OnOffServer.class.getName());
			
			if (cluster != null)
			{
				// store the cluster
				this.onOffClusterServer = cluster;
				
				// stop the iteration as the needed cluster has been found
				break;
			}
		}
		// call the superclass method
		super.setIAppliance(appliance);
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
	private void changeCurrentState(String OnOffValue)
	{
		String currentStateValue = (String) this.currentState.getState(OnOffState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
		// if the current states it is different from the new state
		if (!currentStateValue.equalsIgnoreCase(OnOffValue))
		{
			State newState;
			// set the new state to on or off...
			if (OnOffValue.equalsIgnoreCase(OnOffState.ON))
			{
				newState = new OnOffState(new OffStateValue());
			}
			else
			{
				newState = new OnOffState(new OnStateValue());
			}
			// ... then set the new state for the device and throw a state
			// changed notification
			this.currentState.setState(newState.getStateName(), newState);
			this.notifyStateChanged(newState);
		}
		
	}
	
}
