/*
 * Dog 2.0 - ZigBee Movement Driver
 * 
 * Copyright 2013 Dario Bonino 
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
package it.polito.elite.dog.drivers.zigbee.movementsensor.cluster;

import it.polito.elite.dog.core.library.model.state.MovementState;
import it.polito.elite.dog.core.library.model.statevalue.MovingStateValue;
import it.polito.elite.dog.drivers.zigbee.movementsensor.ZigBeeMovementSensorDriverInstance;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IEndPointRequestContext;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.Appliance;
import it.telecomitalia.ah.hac.lib.ServiceCluster;

/**
 * @author bonino
 * 
 */
public class ZigBeeMovementSensorOnOffServerCluster extends ServiceCluster
		implements OnOffServer
{
	// The instance of ZigBeeDoorWindowSensorDriverInstance that will handle
	// commands sent to this server cluster
	private ZigBeeMovementSensorDriverInstance theInstance;

	/**
	 * @throws ApplianceException
	 * 
	 */
	public ZigBeeMovementSensorOnOffServerCluster(
			ZigBeeMovementSensorDriverInstance instance, Appliance appliance)
			throws ApplianceException
	{
		super();
		this.theInstance = instance;
		super.appliance = appliance;
	}

	@Override
	public boolean getOnOff(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		if (this.theInstance.getState()
				.getState(MovementState.class.getSimpleName())
				.getCurrentStateValue()[0] instanceof MovingStateValue)
			return true;
		else
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
		this.theInstance.notifyCeasedMovement();

	}

	@Override
	public void execOn(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// handle on
		this.theInstance.notifyDetectedMovement();

	}

	@Override
	public void execToggle(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		if (this.theInstance.getState()
				.getState(MovementState.class.getSimpleName())
				.getCurrentStateValue()[0] instanceof MovingStateValue)
			this.theInstance.notifyCeasedMovement();
		else
			this.theInstance.notifyDetectedMovement();

	}

	@Override
	public void execOnWithDuration(int OnDuration,
			IEndPointRequestContext context) throws ApplianceException,
			ServiceClusterException
	{
		// TODO Auto-generated method stub

	}

}
