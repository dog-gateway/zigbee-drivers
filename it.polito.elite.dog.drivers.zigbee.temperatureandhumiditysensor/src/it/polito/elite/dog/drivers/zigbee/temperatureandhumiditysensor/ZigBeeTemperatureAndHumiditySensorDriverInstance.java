/*
 * Dog 2.0 - ZigBee TemperatureAndHumiditySensor Driver
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
package it.polito.elite.dog.drivers.zigbee.temperatureandhumiditysensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.TemperatureAndHumiditySensor;
import it.polito.elite.dog.core.library.model.state.HumidityMeasurementState;
import it.polito.elite.dog.core.library.model.state.TemperatureState;
import it.polito.elite.dog.core.library.model.statevalue.HumidityStateValue;
import it.polito.elite.dog.core.library.model.statevalue.TemperatureStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import org.energy_home.jemma.ah.cluster.zigbee.measurement.RelativeHumidityMeasurementServer;
import org.energy_home.jemma.ah.cluster.zigbee.measurement.TemperatureMeasurementServer;
import org.energy_home.jemma.ah.hac.ApplianceException;
import org.energy_home.jemma.ah.hac.IAttributeValue;
import org.energy_home.jemma.ah.hac.IEndPoint;
import org.energy_home.jemma.ah.hac.IEndPointRequestContext;
import org.energy_home.jemma.ah.hac.IServiceCluster;
import org.energy_home.jemma.ah.hac.ISubscriptionParameters;
import org.energy_home.jemma.ah.hac.ServiceClusterException;
import org.energy_home.jemma.ah.hac.lib.SubscriptionParameters;

import java.util.HashSet;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Actual driver for ZigBee Temperature and Humidity sensors.
 * @author bonino
 * 
 */
public class ZigBeeTemperatureAndHumiditySensorDriverInstance extends
		ZigBeeDriverInstance implements TemperatureAndHumiditySensor
{

	// the class logger
	private LogHelper logger;

	// the temperature measurement cluster associated to the appliance managed
	// by this driver
	private TemperatureMeasurementServer temperatureMeasurementClusterServer;

	// the humidity measurement cluster associated to the appliance managed by
	// this driver
	private RelativeHumidityMeasurementServer relativeHumidityMeasurementClusterServer;

	// the kW divisor
	private int divisor = 100;
	private int multiplier = 1;

	// the reporting time to set
	private int reportingTimeSeconds;
	
	// the group set
		private HashSet<Integer> groups;

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
	public ZigBeeTemperatureAndHumiditySensorDriverInstance(
			ZigBeeNetwork network, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds)
	{
		super(network, device);

		// create a logger
		this.logger = new LogHelper(context);

		// initialize the reporting time
		this.reportingTimeSeconds = reportingTimeSeconds;
		
		// build inner data structures
				this.groups = new HashSet<Integer>();

		// read the initial state of devices
		this.initializeStates();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver#setIAppliance
	 * (it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo)
	 */
	@Override
	public synchronized void setIAppliance(ZigBeeApplianceInfo appliance)
	{
		// call the superclass method
		super.setIAppliance(appliance);

		// reset the clusters
		this.temperatureMeasurementClusterServer = null;

		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();

		for (IEndPoint endpoint : endpoints)
		{
			// try to attach the SimpleMeteringCluster associated to the
			// appliance
			this.attachTemperatureMeasurementCluster(endpoint);

			// try to attach the SimpleMeteringCluster associated to the
			// appliance
			this.attachRelativeHumidityMeasurementCluster(endpoint);

			if ((this.temperatureMeasurementClusterServer != null)&&(this.relativeHumidityMeasurementClusterServer!=null))
				break;
		}

		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeTemperatureAndHumiditySensorDriver.logId
						+ "Subscribed to all clusters");
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
	protected void newMessageFromHouse(Integer endPointId, String clusterName,
			String attributeName, IAttributeValue attributeValue)
	{
		// handle SimpleMeteringCluster only...
		if (clusterName.equals(TemperatureMeasurementServer.class.getName()))
		{
			// handle metering notifications
			if (attributeName
					.equals(TemperatureMeasurementServer.ATTR_MeasuredValue_NAME))
			{
				// active power notification

				// conversion as dictated by the metering cluster specification
				int valueAsInt = (Integer) attributeValue.getValue();
				double valueAsCelsius = ((double) valueAsInt * (double) this.multiplier)
						/ (double) this.divisor;

				// notify the new value
				this.notifyNewTemperatureValue(DecimalMeasure
						.valueOf(valueAsCelsius + " " + SI.CELSIUS.toString()));
				
				//notify the state change
				this.updateStatus();

			}
		}
		else if (clusterName.equals(RelativeHumidityMeasurementServer.class
				.getName()))
		{
			// handle metering notifications
			if (attributeName
					.equals(RelativeHumidityMeasurementServer.ATTR_MeasuredValue_NAME))
			{
				// active power notification

				// conversion as dictated by the metering cluster specification
				int valueAsInt = (Integer) attributeValue.getValue();
				double valueAsPercentage = ((double) valueAsInt * (double) this.multiplier)
						/ (double) this.divisor;

				// notify the new value
				this.notifyChangedRelativeHumidity(DecimalMeasure
						.valueOf(valueAsPercentage + " %"));

			}
		}

	}

	/**
	 * Initialize the device state...
	 */
	private void initializeStates()
	{

		// create the var and va units
		Unit.ONE.alternate("%");

		// initialize the state
		this.currentState.setState(TemperatureState.class.getSimpleName(),
				new TemperatureState(new TemperatureStateValue()));
		this.currentState.setState(
				HumidityMeasurementState.class.getSimpleName(),
				new HumidityMeasurementState(new HumidityStateValue()));

	}

	/**
	 * Attaches to Meter notifications (every
	 * 
	 * <pre>
	 * reportingTimeSeconds
	 * </pre>
	 * 
	 * @param endpoint
	 *            the endpoint for which trying to attach the notifications
	 * @return true if successful, false otherwise
	 */
	private boolean attachTemperatureMeasurementCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint
				.getServiceCluster(TemperatureMeasurementServer.class.getName());

		if (cluster != null)
		{
			// store the cluster
			this.temperatureMeasurementClusterServer = (TemperatureMeasurementServer) cluster;

			// set the attribute subscription
			try
			{
				// get the request context to send attribute subscription
				// requests
				IEndPointRequestContext reqContext = this.theManagedAppliance
						.getEndpoint().getDefaultRequestContext();

				// get current subscriptions to avoid subscribing to already
				// subscribed attributes
				ISubscriptionParameters acceptedParams = cluster
						.getAttributeSubscription(
								TemperatureMeasurementServer.ATTR_MeasuredValue_NAME,
								reqContext);

				// perform attribute subscription and get the accepted
				// subscription parameters for active energy
				if (acceptedParams == null)
				{
					acceptedParams = cluster
							.setAttributeSubscription(
									TemperatureMeasurementServer.ATTR_MeasuredValue_NAME,
									new SubscriptionParameters(
											this.reportingTimeSeconds,
											this.reportingTimeSeconds, 0),
									reqContext);

					// debug
					this.debugSubscription(acceptedParams);
				}

			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeTemperatureAndHumiditySensorDriver.logId
										+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeTemperatureAndHumiditySensorDriver.logId
										+ "Error (ServiceClusterException) while setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}

			done = true;
		}

		return done;
	}

	/**
	 * Attaches to Meter notifications (every
	 * 
	 * <pre>
	 * reportingTimeSeconds
	 * </pre>
	 * 
	 * @param endpoint
	 *            the endpoint for which trying to attach the notifications
	 * @return true if successful, false otherwise
	 */
	private boolean attachRelativeHumidityMeasurementCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint
				.getServiceCluster(RelativeHumidityMeasurementServer.class
						.getName());

		if (cluster != null)
		{
			// store the cluster
			this.relativeHumidityMeasurementClusterServer = (RelativeHumidityMeasurementServer) cluster;

			// set the attribute subscription
			try
			{
				// get the request context to send attribute subscription
				// requests
				IEndPointRequestContext reqContext = this.theManagedAppliance
						.getEndpoint().getDefaultRequestContext();

				// get current subscriptions to avoid subscribing to already
				// subscribed attributes
				ISubscriptionParameters acceptedParams = cluster
						.getAttributeSubscription(
								RelativeHumidityMeasurementServer.ATTR_MeasuredValue_NAME,
								reqContext);

				// perform attribute subscription and get the accepted
				// subscription parameters for active energy
				if (acceptedParams == null)
				{
					acceptedParams = cluster
							.setAttributeSubscription(
									RelativeHumidityMeasurementServer.ATTR_MeasuredValue_NAME,
									new SubscriptionParameters(
											this.reportingTimeSeconds,
											this.reportingTimeSeconds, 0),
									reqContext);

					// debug
					this.debugSubscription(acceptedParams);
				}

			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeTemperatureAndHumiditySensorDriver.logId
										+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeTemperatureAndHumiditySensorDriver.logId
										+ "Error (ServiceClusterException) while setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}

			done = true;
		}

		return done;
	}

	private void debugSubscription(ISubscriptionParameters acceptedParams)
	{
		// debug
		if (this.logger != null)
		{
			if (acceptedParams != null)
				this.logger.log(
						LogService.LOG_DEBUG,
						ZigBeeTemperatureAndHumiditySensorDriver.logId
								+ "Subscription result:"
								+ acceptedParams.getMinReportingInterval()
								+ ","
								+ acceptedParams.getMaxReportingInterval()
								+ "," + acceptedParams.getReportableChange());
			else
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeTemperatureAndHumiditySensorDriver.logId
								+ "Subscripion not accepted");
		}
	}


	@Override
	public void deleteGroup(Integer groupID)
	{
		// remove the given group id
		this.groups.remove(groupID);
		
		//notify
		this.notifyLeftGroup(groupID);
	}

	@Override
	public void storeGroup(Integer groupID)
	{
		// Store the given group id
		this.groups.add(groupID);
		
		this.notifyJoinedGroup(groupID);
	}
	
	@Override
	public DeviceStatus getState()
	{
		return currentState;
	}
	
	@Override
	public Measure<?, ?> getRelativeHumidity()
	{
		return (Measure<?, ?>) currentState.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}
	
	@Override
	public Measure<?, ?> getTemperature()
	{
		return (Measure<?, ?>) currentState.getState(TemperatureState.class.getSimpleName()).getCurrentStateValue()[0]
				.getValue();
	}
	
	@Override
	public void notifyNewTemperatureValue(Measure<?, ?> temperatureValue)
	{
		// if the given temperature is null, than the network-level value is not
		// up-to-date
		// and the currently stored value should be notified
		if (temperatureValue != null)
		{
			// update the state
			TemperatureStateValue pValue = new TemperatureStateValue();
			pValue.setValue(temperatureValue);
			currentState.setState(TemperatureState.class.getSimpleName(), new TemperatureState(pValue));
		}
		else
		{
			
			temperatureValue = (Measure<?, ?>) this.currentState.getState(TemperatureState.class.getSimpleName())
					.getCurrentStateValue()[0].getValue();
		}
		// debug
		logger.log(LogService.LOG_DEBUG,
				ZigBeeTemperatureAndHumiditySensorDriver.logId + "Device " + device.getDeviceId() + " temperature "
						+ temperatureValue.toString());
		
		// notify the new measure
		((TemperatureAndHumiditySensor) device).notifyNewTemperatureValue(temperatureValue);
	}
	
	@Override
	public void notifyChangedRelativeHumidity(Measure<?, ?> relativeHumidity)
	{
		// if the given temperature is null, than the network-level value is not
		// up-to-date
		// and the currently stored value should be notified
		if (relativeHumidity != null)
		{
			// update the state
			HumidityStateValue pValue = new HumidityStateValue();
			pValue.setValue(relativeHumidity);
			currentState.setState(HumidityMeasurementState.class.getSimpleName(), new HumidityMeasurementState(pValue));
		}
		else
		{
			relativeHumidity = (Measure<?, ?>) this.currentState.getState(
					HumidityMeasurementState.class.getSimpleName()).getCurrentStateValue()[0].getValue();
		}
		// debug
		logger.log(LogService.LOG_DEBUG,
				ZigBeeTemperatureAndHumiditySensorDriver.logId + "Device " + device.getDeviceId() + " humidity "
						+ relativeHumidity.toString());
		
		// notify the new measure
		((TemperatureAndHumiditySensor) device).notifyChangedRelativeHumidity(relativeHumidity);
	}
	
	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		// send the joined group notification
		((TemperatureAndHumiditySensor)this.device).notifyJoinedGroup(groupNumber);

	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		// send the left group notification
		((TemperatureAndHumiditySensor)this.device).notifyLeftGroup(groupNumber);

	}

	@Override
	public void updateStatus()
	{
		((Controllable)this.device).updateStatus();
	}

}
