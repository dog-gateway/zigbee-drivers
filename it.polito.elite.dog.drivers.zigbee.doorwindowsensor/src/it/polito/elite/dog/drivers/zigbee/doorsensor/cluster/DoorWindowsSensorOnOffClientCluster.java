/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.doorsensor.cluster;

import it.polito.elite.dog.core.library.model.state.OpenCloseState;
import it.polito.elite.dog.core.library.model.statevalue.OpenStateValue;
import it.polito.elite.dog.drivers.zigbee.doorsensor.ZigBeeDoorWindowSensorDriverInstance;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IEndPointRequestContext;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.ServiceCluster;

/**
 * @author bonino
 * 
 */
public class DoorWindowsSensorOnOffClientCluster extends ServiceCluster implements OnOffServer
{
	// The instance of ZigBeeDoorWindowSensorDriverInstance that will handle
	// commands sent to this server cluster
	private ZigBeeDoorWindowSensorDriverInstance theInstance;

	/**
	 * @throws ApplianceException 
	 * 
	 */
	public DoorWindowsSensorOnOffClientCluster(
			ZigBeeDoorWindowSensorDriverInstance instance) throws ApplianceException
	{
		super();
		this.theInstance = instance;
	}

	@Override
	public boolean getOnOff(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getMaxOnDuration(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentOnDuration(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void execOff(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// handle off
		this.theInstance.notifyClose();

	}

	@Override
	public void execOn(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// handle on
		this.theInstance.notifyOpen();

	}
	
	

	@Override
	public void execToggle(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		if (this.theInstance.getState()
				.getState(OpenCloseState.class.getSimpleName())
				.getCurrentStateValue()[0] instanceof OpenStateValue)
			this.theInstance.notifyClose();
		else
			this.theInstance.notifyOpen();

	}

	@Override
	public void execOnWithDuration(int OnDuration,
			IEndPointRequestContext context) throws ApplianceException,
			ServiceClusterException
	{
		// TODO Auto-generated method stub

	}

}
