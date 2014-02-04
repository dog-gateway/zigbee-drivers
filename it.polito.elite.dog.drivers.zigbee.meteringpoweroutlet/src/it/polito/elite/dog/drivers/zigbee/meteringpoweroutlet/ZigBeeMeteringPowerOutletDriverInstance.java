/*
 * Dog 2.0 - ZigBee MeteringPowerOutlet Driver
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
package it.polito.elite.dog.drivers.zigbee.meteringpoweroutlet;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ElectricalSystem;
import it.polito.elite.dog.core.library.model.devicecategory.MeteringPowerOutlet;
import it.polito.elite.dog.core.library.model.notification.SinglePhaseActiveEnergyMeasurementNotification;
import it.polito.elite.dog.core.library.model.notification.SinglePhaseActivePowerMeasurementNotification;
import it.polito.elite.dog.core.library.model.notification.SinglePhaseReactiveEnergyMeasurementNotification;
import it.polito.elite.dog.core.library.model.state.OnOffState;
import it.polito.elite.dog.core.library.model.state.PowerFactorMeasurementState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseActiveEnergyState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseActivePowerMeasurementState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseReactiveEnergyState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.ActiveEnergyStateValue;
import it.polito.elite.dog.core.library.model.statevalue.ActivePowerStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OffStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OnStateValue;
import it.polito.elite.dog.core.library.model.statevalue.PowerFactorStateValue;
import it.polito.elite.dog.core.library.model.statevalue.ReactiveEnergyStateValue;
import it.polito.elite.dog.core.library.model.statevalue.StateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.CmdNotificationInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.general.OnOffServer;
import it.telecomitalia.ah.cluster.zigbee.metering.SimpleMeteringServer;
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
import javax.measure.quantity.Power;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * The metering power outlet driver instance which actually handles devices
 * classifies as MeteringPowerOutlets, on ZigBee networks.
 * 
 * @author bonino
 * 
 */
public class ZigBeeMeteringPowerOutletDriverInstance extends ZigBeeDriverInstance
		implements MeteringPowerOutlet
{
	// the class logger
	private LogHelper logger;

	// the on-off cluster associated to the appliance managed by this driver
	private OnOffServer onOffClusterServer;

	// the metering cluster associated to the appliance managed by this driver
	private SimpleMeteringServer simpleMeteringClusterServer;

	// the kW divisor
	private int divisor = 1;
	private int multiplier = 1;

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
	public ZigBeeMeteringPowerOutletDriverInstance(ZigBeeNetwork network,
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
		if ((this.theManagedAppliance != null)
				&& (this.onOffClusterServer != null))
		{
			try
			{
				// send the on command to the corresponding cluster
				this.onOffClusterServer.execOn(this.theManagedAppliance
						.getEndpoint().getDefaultRequestContext());

				// temporary shor-circuit for state update
				// this.changeCurrentState(OnOffState.ON);

				// log the command
				this.logger
						.log(LogService.LOG_DEBUG,
								ZigBeeMeteringPowerOutletDriver.logId
										+ " Sent OnCommand to the ZigBee device with serial "
										+ this.theManagedAppliance.getSerial());
			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ApplianceException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ServiceClusterException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
		}
	}

	@Override
	public void off()
	{
		// check if the appliance is already available
		if ((this.theManagedAppliance != null)
				&& (this.onOffClusterServer != null))
		{
			try
			{
				// send the off command to the corresponding cluster
				this.onOffClusterServer.execOff(this.theManagedAppliance
						.getEndpoint().getDefaultRequestContext());

				// temporary short-circuit for state update
				// this.changeCurrentState(OnOffState.OFF);

				// log the command
				this.logger
						.log(LogService.LOG_DEBUG,
								ZigBeeMeteringPowerOutletDriver.logId
										+ " Sent OffCommand to the ZigBee device with serial "
										+ this.theManagedAppliance.getSerial());
			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ApplianceException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ServiceClusterException) while sending OnCommand to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
		}

	}

	@Override
	public void notifyStateChanged(State newState)
	{
		((ElectricalSystem) this.device).notifyStateChanged(newState);

	}

	@Override
	public Measure<?, ?> getReactiveEnergyValue()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Measure<?, ?> getPowerFactor()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Measure<?, ?> getActiveEnergyValue()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Measure<?, ?> getActivePower()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void notifyNewActivePowerValue(Measure<?, ?> powerValue)
	{
		StateValue currentStateValue = this.currentState.getState(
				SinglePhaseActivePowerMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0];

		// convert the value to the currently set value
		powerValue = powerValue.to(((Measure) (currentStateValue.getValue()))
				.getUnit());

		// update the state
		currentStateValue.setValue(powerValue);

		// notify the new measure
		((MeteringPowerOutlet) this.device)
				.notifyNewActivePowerValue(powerValue);


		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId
						+ "Notifying power value: " + powerValue);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void notifyNewReactiveEnergyValue(Measure<?, ?> value)
	{
		StateValue currentStateValue = this.currentState.getState(
				SinglePhaseReactiveEnergyState.class.getSimpleName())
				.getCurrentStateValue()[0];

		// convert the value to the currently set value
		value = value.to(((DecimalMeasure) currentStateValue.getValue())
				.getUnit());

		currentStateValue.setValue(value);

		// notify the new measure
		((MeteringPowerOutlet) this.device).notifyNewReactiveEnergyValue(value);

		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId
						+ "Notifying reactive energy value: " + value);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void notifyNewActiveEnergyValue(Measure<?, ?> value)
	{
		StateValue currentStateValue = this.currentState.getState(
				SinglePhaseActiveEnergyState.class.getSimpleName())
				.getCurrentStateValue()[0];

		// convert the value to the currently set value
		value = value.to(((DecimalMeasure) currentStateValue.getValue())
				.getUnit());

		currentStateValue.setValue(value);

		// notify the new measure
		((MeteringPowerOutlet) this.device).notifyNewActiveEnergyValue(value);


		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId
						+ "Notifying active energy value: " + value);

	}

	@Override
	public void notifyNewPowerFactorValue(Measure<?, ?> powerFactor)
	{
		// update the state

		this.currentState.getState(
				PowerFactorMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(powerFactor);

		// notify the new measure
		((MeteringPowerOutlet) this.device)
				.notifyNewPowerFactorValue(powerFactor);
		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId
						+ "Notifying power factor value: " + powerFactor);

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
		this.onOffClusterServer = null;
		this.simpleMeteringClusterServer = null;

		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();

		for (IEndPoint endpoint : endpoints)
		{
			// try to attach the OnOffCluster associated to the appliance
			this.attachOnOffCluster(endpoint);

			// try to attach the SimpleMeteringCluster associated to the
			// appliance
			this.attachSimpleMeteringCluster(endpoint);

			if ((this.onOffClusterServer != null)
					&& (this.simpleMeteringClusterServer != null))
				break;
		}

		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId
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
		// handle OnOffCluster only...
		if ((clusterName.equals(OnOffServer.class.getName()))
				&& (attributeName.equals(OnOffServer.ATTR_OnOff_NAME)))
		{
			// translate true and false to on and off
			boolean on = (Boolean) attributeValue.getValue();

			if (on)
				this.changeCurrentState(OnOffState.ON);
			else
				this.changeCurrentState(OnOffState.OFF);
		}
		else if (clusterName.equals(SimpleMeteringServer.class.getName()))
		{
			// handle metering notifications
			if (attributeName
					.equals(SimpleMeteringServer.ATTR_IstantaneousDemand_NAME))
			{
				// active power notification
				this.logger.log(
						LogService.LOG_DEBUG,
						ZigBeeMeteringPowerOutletDriver.logId
								+ "Attribute Value =>"
								+ attributeValue.getValue());

				// conversion as dictated by the metering cluster specification
				int valueAsInt = (Integer) attributeValue.getValue();
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeMeteringPowerOutletDriver.logId + "Value =>"
								+ valueAsInt);

				double valueAskW = ((double) valueAsInt * (double) this.multiplier)
						/ (double) this.divisor;

				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeMeteringPowerOutletDriver.logId + "Value =>"
								+ valueAskW);
				// notify the new value
				this.notifyNewActivePowerValue(DecimalMeasure.valueOf(valueAskW
						+ " " + SI.KILO(SI.WATT).toString()));

			}
			else if (attributeName
					.equals(SimpleMeteringServer.ATTR_CurrentSummationDelivered_NAME))
			{
				// active power notification

				// conversion as dictated by the metering cluster specification
				Long valueAsInt = (Long) attributeValue.getValue();
				double valueAskWh = ((double) valueAsInt * (double) this.multiplier)
						/ (double) this.divisor;

				// notify the new value
				this.notifyNewActiveEnergyValue(DecimalMeasure
						.valueOf(valueAskWh + " "
								+ SI.KILO(SI.WATT.times(NonSI.HOUR)).toString()));

			}
			else if (attributeName
					.equals(SimpleMeteringServer.ATTR_PowerFactor_NAME))
			{
				double powerFactor = (double) ((Short) attributeValue
						.getValue()) / 100.0;
				// notify the new value
				this.notifyNewPowerFactorValue(DecimalMeasure
						.valueOf(powerFactor + " " + Unit.ONE));

			}

		}
		
		
		this.notifyStateChanged(null);

	}

	/**
	 * Initialize the device state...
	 */
	private void initializeStates()
	{

		// create the var and va units
		Unit<Power> VAR = SI.WATT.alternate("var");

		// the base unit of measure
		String activePowerUOM = SI.WATT.toString();
		String reactiveEnergyUOM = (VAR.times(NonSI.HOUR)).toString();
		String activeEnergyUOM = (SI.WATT.times(NonSI.HOUR)).toString();

		// add unit of measure aliases (to fix notation problems...)
		UnitFormat uf = UnitFormat.getInstance();
		uf.alias(SI.WATT.times(NonSI.HOUR), "Wh");
		uf.label(SI.KILO(SI.WATT.times(NonSI.HOUR)), "kWh");
		uf.alias(VAR.times(NonSI.HOUR), "Varh");
		uf.label(SI.KILO(VAR.times(NonSI.HOUR)), "kVarh");

		// get the unit of measure (if configured)
		for (String notificationName : this.notifications.keySet())
		{
			// get the notification description
			CmdNotificationInfo notificationInfo = this.notifications
					.get(notificationName);

			if (notificationInfo
					.getName()
					.equalsIgnoreCase(
							SinglePhaseActivePowerMeasurementNotification.notificationName))
			{
				activePowerUOM = notificationInfo.getParameters().get(
						ZigBeeInfo.ZIGBEE_UOM);
			}
			else if (notificationInfo
					.getName()
					.equalsIgnoreCase(
							SinglePhaseActiveEnergyMeasurementNotification.notificationName))
			{
				activeEnergyUOM = notificationInfo.getParameters().get(
						ZigBeeInfo.ZIGBEE_UOM);
			}
			else if (notificationInfo
					.getName()
					.equalsIgnoreCase(
							SinglePhaseReactiveEnergyMeasurementNotification.notificationName))
			{
				reactiveEnergyUOM = notificationInfo.getParameters().get(
						ZigBeeInfo.ZIGBEE_UOM);
			}
		}

		// set the initial state at off
		this.currentState.setState(OnOffState.class.getSimpleName(),
				new OnOffState(new OffStateValue()));

		// active power state
		ActivePowerStateValue initialActivePowerValue = new ActivePowerStateValue();
		initialActivePowerValue.setValue(DecimalMeasure.valueOf("0 "
				+ activePowerUOM));
		this.currentState.setState(SinglePhaseActivePowerMeasurementState.class
				.getSimpleName(), new SinglePhaseActivePowerMeasurementState(
				initialActivePowerValue));

		// active energy state
		ActiveEnergyStateValue initialActiveEnergyValue = new ActiveEnergyStateValue();
		initialActiveEnergyValue.setValue(DecimalMeasure.valueOf("0 "
				+ activeEnergyUOM));
		this.currentState.setState(
				SinglePhaseActiveEnergyState.class.getSimpleName(),
				new SinglePhaseActiveEnergyState(initialActiveEnergyValue));

		// power factor state
		PowerFactorStateValue initialPowerFactorValue = new PowerFactorStateValue();
		initialPowerFactorValue.setValue(DecimalMeasure
				.valueOf("0 " + Unit.ONE));
		this.currentState.setState(
				PowerFactorMeasurementState.class.getSimpleName(),
				new PowerFactorMeasurementState(initialPowerFactorValue));

		// reactive energy state
		ReactiveEnergyStateValue initialReactiveEnergyValue = new ReactiveEnergyStateValue();
		initialReactiveEnergyValue.setValue(DecimalMeasure.valueOf("0 "
				+ reactiveEnergyUOM));
		this.currentState.setState(
				SinglePhaseReactiveEnergyState.class.getSimpleName(),
				new SinglePhaseReactiveEnergyState(initialReactiveEnergyValue));

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
		String currentStateValue = (String) this.currentState.getState(
				OnOffState.class.getSimpleName()).getCurrentStateValue()[0]
				.getValue();
		// debug
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeMeteringPowerOutletDriver.logId + "Received state :"
						+ onOffValue);

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
			// ... then set the new state for the device 
			this.currentState.setState(newState.getStateName(), newState);
		}

	}

	/**
	 * Attaches to OnOff notifications (on every state change and every
	 * 
	 * <pre>
	 * reportingTimeSeconds
	 * </pre>
	 * 
	 * @param endpoint
	 *            the endpoint for which trying to attach the notifications
	 * @return true if successful, false otherwise
	 */
	private boolean attachOnOffCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint.getServiceCluster(OnOffServer.class
				.getName());

		if (cluster != null)
		{
			// store the cluster
			this.onOffClusterServer = (OnOffServer) cluster;

			// set the attribute subscription for on/off state changes
			try
			{
				IEndPointRequestContext reqContext = this.theManagedAppliance
						.getEndpoint().getDefaultRequestContext();
				cluster.setAttributeSubscription(OnOffServer.ATTR_OnOff_NAME,
						new SubscriptionParameters(0,
								this.reportingTimeSeconds, 0), reqContext);
			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
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
	private boolean attachSimpleMeteringCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint
				.getServiceCluster(SimpleMeteringServer.class.getName());

		if (cluster != null)
		{
			// store the cluster
			this.simpleMeteringClusterServer = (SimpleMeteringServer) cluster;

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
								SimpleMeteringServer.ATTR_CurrentSummationDelivered_NAME,
								reqContext);

				// perform attribute subscription and get the accepted
				// subscription parameters for active energy
				if (acceptedParams == null)
				{
					acceptedParams = cluster
							.setAttributeSubscription(
									SimpleMeteringServer.ATTR_CurrentSummationDelivered_NAME,
									new SubscriptionParameters(
											this.reportingTimeSeconds,
											this.reportingTimeSeconds, 0),
									reqContext);

					// debug
					this.debugSubscription(acceptedParams);
				}

				// get current subscriptions to avoid subscribing to already
				// subscribed attributes
				acceptedParams = cluster.getAttributeSubscription(
						SimpleMeteringServer.ATTR_IstantaneousDemand_NAME,
						reqContext);
				// perform attribute subscription and get the accepted
				// subscription parameters for active power
				if (acceptedParams == null)
				{
					acceptedParams = cluster.setAttributeSubscription(
							SimpleMeteringServer.ATTR_IstantaneousDemand_NAME,
							new SubscriptionParameters(
									this.reportingTimeSeconds,
									this.reportingTimeSeconds, 0), reqContext);
					// debug
					this.debugSubscription(acceptedParams);
				}

				// get current subscriptions to avoid subscribing to already
				// subscribed attributes
				acceptedParams = cluster.getAttributeSubscription(
						SimpleMeteringServer.ATTR_PowerFactor_NAME, reqContext);
				// perform attribute subscription and get the accepted
				// subscription parameters for power factor
				if (acceptedParams == null)
				{
					acceptedParams = cluster.setAttributeSubscription(
							SimpleMeteringServer.ATTR_PowerFactor_NAME,
							new SubscriptionParameters(
									this.reportingTimeSeconds,
									this.reportingTimeSeconds, 0), reqContext);
					// debug
					this.debugSubscription(acceptedParams);
				}

				// ------ get divisor and multiplier ----

				// get the divisor needed to convert measured power to W or Wh
				IAttributeValue divisorValue = cluster.getAttributeValue(
						SimpleMeteringServer.ATTR_Divisor_NAME, reqContext);

				// the received divisor value
				int divisor = ((Integer) divisorValue.getValue()).intValue();

				// get the multiplier needed to convert measured power to W or
				// Wh
				IAttributeValue multiplierValue = cluster.getAttributeValue(
						SimpleMeteringServer.ATTR_Multiplier_NAME, reqContext);

				// the received multiplier value
				int multiplier = ((Integer) multiplierValue.getValue())
						.intValue();

				// log the multiplier and divisor
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeMeteringPowerOutletDriver.logId + "Divisor["
								+ this.divisor + "]<=" + divisor
								+ " Multiplier[" + this.multiplier + "]<="
								+ multiplier);

				// ------ update divisor and multiplier ----

				// update if greater than 1 and different from the one currently
				// stored
				if ((divisor > 1) && (divisor != this.divisor))
					this.divisor = (Integer) divisorValue.getValue();

				// update if greater than 1 and different from the one currently
				// stored
				if ((multiplier > 1) && (multiplier != this.multiplier))
				{
					this.multiplier = (Integer) multiplierValue.getValue();
				}

			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
										+ "Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								ZigBeeMeteringPowerOutletDriver.logId
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
		if (acceptedParams != null)
			this.logger.log(
					LogService.LOG_DEBUG,
					ZigBeeMeteringPowerOutletDriver.logId
							+ "Subscription result:"
							+ acceptedParams.getMinReportingInterval() + ","
							+ acceptedParams.getMaxReportingInterval() + ","
							+ acceptedParams.getReportableChange());
		else
			this.logger.log(LogService.LOG_DEBUG,
					ZigBeeMeteringPowerOutletDriver.logId
							+ "Subscription not accepted");
	}
}
