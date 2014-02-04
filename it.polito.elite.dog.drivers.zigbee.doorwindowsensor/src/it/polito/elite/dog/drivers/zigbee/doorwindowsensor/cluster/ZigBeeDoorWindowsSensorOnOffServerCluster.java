/*
 * Dog 2.0 - ZigBee Door and Window Sensor Driver
 * 
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
package it.polito.elite.dog.drivers.zigbee.doorwindowsensor.cluster;

import it.polito.elite.dog.core.library.model.state.OpenCloseState;
import it.polito.elite.dog.core.library.model.statevalue.OpenStateValue;
import it.polito.elite.dog.drivers.zigbee.doorwindowsensor.ZigBeeDoorWindowSensorDriverInstance;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IEndPointRequestContext;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.Appliance;
import it.telecomitalia.ah.hac.lib.ServiceCluster;

/**
 * Implements an {@link OnOffServer} cluster connected to a
 * {@link ZigBeeDoorWindowSensorDriverInstance}. This allows capturing requests
 * of physical clients and to boind them to open/close primitives offered by the
 * driver instance.
 * 
 * @author bonino
 * 
 */
public class ZigBeeDoorWindowsSensorOnOffServerCluster extends ServiceCluster
		implements OnOffServer
{
	// The instance of ZigBeeDoorWindowSensorDriverInstance that will handle
	// commands sent to this server cluster
	private ZigBeeDoorWindowSensorDriverInstance theInstance;

	/**
	 * Constructor, initializes inner data structures
	 * @throws ApplianceException
	 * 
	 */
	public ZigBeeDoorWindowsSensorOnOffServerCluster(
			ZigBeeDoorWindowSensorDriverInstance instance, Appliance appliance)
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
				.getState(OpenCloseState.class.getSimpleName())
				.getCurrentStateValue()[0] instanceof OpenStateValue)
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
