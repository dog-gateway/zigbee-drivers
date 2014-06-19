/*
 * Dog 2.0 - ZigBee MeterBridge
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
package it.polito.elite.dog.drivers.zigbee.meterbridge.cluster;

import it.polito.elite.dog.core.library.model.AbstractDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.EnergyAndPowerMeter;
import it.polito.elite.dog.core.library.model.devicecategory.MeteringPowerOutlet;
import it.polito.elite.dog.core.library.model.state.PowerFactorMeasurementState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseActiveEnergyState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseActivePowerMeasurementState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.meterbridge.appliance.BridgedMeteringAppliance;
import org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer;
import org.energy_home.jemma.ah.hac.ApplianceException;
import org.energy_home.jemma.ah.hac.IEndPointRequestContext;
import org.energy_home.jemma.ah.hac.ServiceClusterException;
import org.energy_home.jemma.ah.hac.lib.ServiceCluster;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.osgi.service.log.LogService;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.service.monitor.StatusVariable;

/**
 * Implements a SimpleMeteringServer cluster bound to the state of a Meter
 * device ({@link MeteringPowerOutlet} or {@link EnergyAndPowerMeter}).
 * 
 * @author bonino
 * 
 */
public class MeterBridgeSimpleMeteringServerCluster extends ServiceCluster
		implements SimpleMeteringServer
{

	// the reference to the Monitor Admin service to check the current state of
	// the associated device
	private MonitorAdmin monitorAdmin;

	// the logger
	private LogHelper logger;

	// the bridged device
	private String deviceId;

	// precision: 0.1 W. Value in kW value in vInt * 1/10000 therefore
	// given the current state value in W -
	// value in W = vInt/10 --> vInt = value(W)*10

	// the multiplier
	private int multiplier = 1;

	// the divisor
	private int divisor = 10000;

	public MeterBridgeSimpleMeteringServerCluster(MonitorAdmin monitorAdmin,
			String deviceId, BridgedMeteringAppliance appliance,
			LogHelper logger) throws ApplianceException
	{
		super();
		super.appliance = appliance;

		// store a reference to the logger
		this.logger = logger;

		// store a reference to the monitor admin service
		this.monitorAdmin = monitorAdmin;

		// store a reference to the deviceId, can only be a metering power
		// outlet or a single phase energy meter
		this.deviceId = deviceId;
	}

	@Override
	public long getCurrentSummationDelivered(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		return this.getMeasureAsLongForState(
				SinglePhaseActiveEnergyState.class.getSimpleName(), 10);
	}

	@Override
	public long getCurrentSummationReceived(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getPowerFactor(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		return (short) this.getMeasureAsLongForState(
				PowerFactorMeasurementState.class.getSimpleName(), 100);
	}

	@Override
	public short getStatus(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getUnitOfMeasure(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMultiplier(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return this.multiplier;
	}

	@Override
	public int getDivisor(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return this.divisor;
	}

	@Override
	public short getSummationFormatting(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getDemandFormatting(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getMeteringDeviceType(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getIstantaneousDemand(IEndPointRequestContext context)
			throws ApplianceException, ServiceClusterException
	{
		return (int) this.getMeasureAsLongForState(
				SinglePhaseActivePowerMeasurementState.class.getSimpleName(),
				10);
	}

	/**
	 * Given a State name and a multiplier provides back the State value as a
	 * long. Value is computed by multplying the actual value with the given
	 * multiplier.
	 * 
	 * @param stateName
	 * @param multiplier
	 * @return
	 */
	private long getMeasureAsLongForState(String stateName, int multiplier)
	{
		long valueAsLong = 0;
		// get the current status from the monitor admin service
		StatusVariable deviceStatusVariable = monitorAdmin
				.getStatusVariable(AbstractDevice
						.toMonitorableId(this.deviceId) + "/status");

		// the status instance holding the actual device status
		DeviceStatus currentDeviceStatus = null;

		try
		{
			// Try the deserialization of the DeviceStatus
			currentDeviceStatus = DeviceStatus
					.deserializeFromString((String) deviceStatusVariable
							.getString());
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					" device status deserialization error "
							+ e.getClass().getSimpleName());
		}

		if (currentDeviceStatus != null)
		{
			// get the energy state
			State currentState = currentDeviceStatus.getState(stateName);
			if (currentState != null)
			{
				DecimalMeasure<?> measure = (DecimalMeasure<?>) currentState
						.getCurrentStateValue()[0].getValue();

				// check the unit of measure
				BigDecimal value = measure.getValue();

				valueAsLong = (long) (value.doubleValue() * multiplier);
			}
		}

		return valueAsLong;
	}
}
