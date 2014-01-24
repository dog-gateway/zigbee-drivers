/*
 * Dog 2.0 - ZigBee EnergyAndPowerMeter Driver
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
package it.polito.elite.dog.drivers.zigbee.lightsensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ElectricalSystem;
import it.polito.elite.dog.core.library.model.devicecategory.LightSensor;
import it.polito.elite.dog.core.library.model.state.LightIntensityState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.LevelStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.measurement.IlluminanceMeasurementServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IAttributeValue;
import it.telecomitalia.ah.hac.IEndPoint;
import it.telecomitalia.ah.hac.IEndPointRequestContext;
import it.telecomitalia.ah.hac.IServiceCluster;
import it.telecomitalia.ah.hac.ISubscriptionParameters;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.SubscriptionParameters;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeLightSensorDriverInstance extends ZigBeeDriverInstance
		implements LightSensor
{

	// the class logger
	private LogHelper logger;

	// the illuminance measurement cluster associated to the appliance managed
	// by this driver
	private IlluminanceMeasurementServer illuminanceMeasurementClusterServer;

	// the kW divisor
	private int divisor = 10000;

	// the reporting time to set
	private int reportingTimeSeconds;

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
	public ZigBeeLightSensorDriverInstance(ZigBeeNetwork network,
			ControllableDevice device, BundleContext context,
			int reportingTimeSeconds)
	{
		super(network, device);

		// create a logger
		this.logger = new LogHelper(context);

		// initialize the reporting time
		this.reportingTimeSeconds = reportingTimeSeconds;

		// read the initial state of devices
		this.initializeStates();
	}

	@Override
	public void notifyStateChanged(State newState)
	{
		// debug
		this.logger.log(
				LogService.LOG_DEBUG,
				ZigBeeLightSensorDriver.logId + "Device "
						+ this.device.getDeviceId() + " is now "
						+ newState.getCurrentStateValue()[0].getValue());
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
	public synchronized void setIAppliance(ZigBeeApplianceInfo appliance)
	{
		// call the superclass method
		super.setIAppliance(appliance);

		// reset the clusters
		this.illuminanceMeasurementClusterServer = null;

		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();

		for (IEndPoint endpoint : endpoints)
		{
			// try to attach the SimpleMeteringCluster associated to the
			// appliance
			this.attachIlluminanceMeasurementCluster(endpoint);

			if (this.illuminanceMeasurementClusterServer != null)
				break;
		}

		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeLightSensorDriver.logId
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
		if (clusterName.equals(IlluminanceMeasurementServer.class.getName()))
		{
			// handle metering notifications
			if (attributeName
					.equals(IlluminanceMeasurementServer.ATTR_MeasuredValue_NAME))
			{
				// active power notification

				// conversion as dictated by the metering cluster specification
				int valueAsInt = (Integer) attributeValue.getValue();
				double valueAsLux = Math.pow(10,
						(((double) valueAsInt) / (double) this.divisor))- 1;

				// notify the new value
				this.notifyNewLuminosityValue(DecimalMeasure.valueOf(valueAsLux
						+ " " + SI.LUX.toString()));

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
		this.currentState.setState(LightIntensityState.class.getSimpleName(),
				new LightIntensityState(new LevelStateValue()));

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
	private boolean attachIlluminanceMeasurementCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint
				.getServiceCluster(IlluminanceMeasurementServer.class.getName());

		if (cluster != null)
		{
			// store the cluster
			this.illuminanceMeasurementClusterServer = (IlluminanceMeasurementServer) cluster;

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
								IlluminanceMeasurementServer.ATTR_MeasuredValue_NAME,
								reqContext);

				// perform attribute subscription and get the accepted
				// subscription parameters for active energy
				if (acceptedParams == null)
				{
					acceptedParams = cluster
							.setAttributeSubscription(
									IlluminanceMeasurementServer.ATTR_MeasuredValue_NAME,
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
								ZigBeeLightSensorDriver.logId
										+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeLightSensorDriver.logId
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
						ZigBeeLightSensorDriver.logId + "Subscription result:"
								+ acceptedParams.getMinReportingInterval()
								+ ","
								+ acceptedParams.getMaxReportingInterval()
								+ "," + acceptedParams.getReportableChange());
			else
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeLightSensorDriver.logId
								+ "Subscripion not accepted");
		}
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
		return currentState;
	}

	@Override
	public Measure<?, ?> getLuminance()
	{
		return (Measure<?, ?>) currentState.getState(
				LightIntensityState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public void notifyNewLuminosityValue(Measure<?, ?> illuminanceValue)
	{
		// if the given illuminance is null, than the network-level value is not
		// up-to-date
		// and the currently stored value should be notified
		if (illuminanceValue != null)
		{
			// update the state
			LevelStateValue pValue = new LevelStateValue();
			pValue.setValue(illuminanceValue);
			currentState.setState(LightIntensityState.class.getSimpleName(),
					new LightIntensityState(pValue));
		}
		else
		{

			illuminanceValue = (Measure<?, ?>) this.currentState.getState(
					LightIntensityState.class.getSimpleName())
					.getCurrentStateValue()[0].getValue();
		}
		// debug
		logger.log(LogService.LOG_DEBUG, ZigBeeLightSensorDriver.logId
				+ "Device " + device.getDeviceId() + " luminosity "
				+ illuminanceValue.toString());

		// notify the new measure
		((LightSensor) device).notifyNewLuminosityValue(illuminanceValue);
	}

}
