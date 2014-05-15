/*
 * Dog 2.0 - ZigBee Door and Window Sensor Driver Instance
 * 
 * 
 * Copyright 2014 Dario Bonino 
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
package it.polito.elite.dog.drivers.zigbee.doorwindowsensor;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.DoorSensor;
import it.polito.elite.dog.core.library.model.devicecategory.WindowSensor;
import it.polito.elite.dog.core.library.model.state.OpenCloseState;
import it.polito.elite.dog.core.library.model.statevalue.CloseStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OpenStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.doorwindowsensor.appliance.ZigBeeDoorWindowSensorAppliance;
import it.polito.elite.dog.drivers.zigbee.doorwindowsensor.cluster.ZigBeeDoorWindowsSensorOnOffServerCluster;
import it.polito.elite.dog.drivers.zigbee.doorwindowsensor.endpoint.ZigBeeDoorWindowSensorEndpoint;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.security.IASZoneServer;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IAppliance;
import it.telecomitalia.ah.hac.IApplicationEndPoint;
import it.telecomitalia.ah.hac.IApplicationService;
import it.telecomitalia.ah.hac.IAttributeValue;
import it.telecomitalia.ah.hac.IEndPoint;
import it.telecomitalia.ah.hac.IEndPointRequestContext;
import it.telecomitalia.ah.hac.IManagedAppliance;
import it.telecomitalia.ah.hac.IServiceCluster;
import it.telecomitalia.ah.hac.ISubscriptionParameters;
import it.telecomitalia.ah.hac.ServiceClusterException;
import it.telecomitalia.ah.hac.lib.SubscriptionParameters;
import it.telecomitalia.ah.hac.lib.ext.IConnectionAdminService;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * <p>
 * A driver instance managing a real device offering {@link DoorSensor} or
 * {@link WindowSensor} capabilities. It registers a dedicated virtual appliance
 * that exposes an OnOff server cluster on the ZigBee network. Such a cluster is
 * programmatically bound to the OnOff client cluster exposed by the device in
 * order to capture door/window events.
 * </p>
 * 
 * @author bonino
 * 
 */
public class ZigBeeDoorWindowSensorDriverInstance extends ZigBeeDriverInstance
		implements DoorSensor, WindowSensor, IApplicationService
{

	// the class logger
	private LogHelper logger;

	// the IASZone cluster associated to the appliance managed
	// by this driver
	private IASZoneServer iasZoneServerCluster;

	// the reporting time to set
	private int reportingTimeSeconds;

	// the set of exported service clusters
	private IServiceCluster[] exportedClusters;

	// the connection admin service
	private IConnectionAdminService connectionAdmin;

	// the published appliance
	private ZigBeeDoorWindowSensorAppliance publishedAppliance;

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
	 * @param connectionAdmin
	 */
	public ZigBeeDoorWindowSensorDriverInstance(ZigBeeNetwork network,
			ControllableDevice device, BundleContext context,
			int reportingTimeSeconds, IConnectionAdminService connectionAdmin)
	{
		super(network, device);

		// create a logger
		this.logger = new LogHelper(context);

		// initialize the reporting time
		this.reportingTimeSeconds = reportingTimeSeconds;

		// store the reference to the connection admin service
		this.connectionAdmin = connectionAdmin;

		// publish the virtual appliance associated to this instance to check
		// the actual status of the connected device.
		try
		{
			// prepare the appliance configuration parameters
			Hashtable<String, Object> config = new Hashtable<String, Object>();
			config.put(IAppliance.APPLIANCE_NAME_PROPERTY,
					"ah.app.doorsensordriver");

			// create the appliance to publish for capturing the door sensor
			// notifications (OnOffClient)
			this.publishedAppliance = new ZigBeeDoorWindowSensorAppliance(
					"ah.app.doorsensor_" + device.getDeviceId(), config);
			// create the needed endpoint
			ZigBeeDoorWindowSensorEndpoint endpoint = new ZigBeeDoorWindowSensorEndpoint(
					"ah.app.doorsensor", this.publishedAppliance);
			// create the OnOffServer cluster which will handle OnOffClient
			// requests
			ZigBeeDoorWindowsSensorOnOffServerCluster cluster = new ZigBeeDoorWindowsSensorOnOffServerCluster(
					this, this.publishedAppliance);

			// add the cluster to the endpoint
			endpoint.addServiceCluster(cluster);

			// add the endpoint to the appliance
			this.publishedAppliance.addEndPoint(endpoint);

			// flag the appliance as available
			this.publishedAppliance.setAvailability(true);

			// register the appliance in the framework
			context.registerService(IManagedAppliance.class.getName(),
					this.publishedAppliance, null);
		}
		catch (ApplianceException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		this.iasZoneServerCluster = null;

		// extract the OnOffCluster, if available
		IEndPoint endpoints[] = appliance.getAppliance().getEndPoints();

		for (IEndPoint endpoint : endpoints)
		{
			// try to attach the SimpleMeteringCluster associated to the
			// appliance
			this.attachIASZoneCluster(endpoint);

			if (this.iasZoneServerCluster != null)
				break;
		}

		// register binding between the appliance that the driver publishes and
		// the appliance representing the door sensor device.
		try
		{
			this.connectionAdmin
					.createConnection(this.publishedAppliance.getPid(),
							this.theManagedAppliance.getAppliance().getPid());
		}
		catch (ApplianceException e)
		{
			this.logger.log(LogService.LOG_WARNING,
					"Unable to bind the driver and the door sensor appliances");
		}

		// debug
		this.logger.log(LogService.LOG_DEBUG, "Subscribed to all clusters");
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
		if (clusterName.equals(IASZoneServer.class.getName()))
		{
			// handle metering notifications
			if (attributeName.equals(IASZoneServer.ATTR_ZoneStatus_NAME))
			{
				// active power notification

				// conversion as dictated by the metering cluster specification
				int valueAsInt = (Integer) attributeValue.getValue();

				// notify the new state
				if (valueAsInt == 1)
					this.notifyOpen();
				else
					this.notifyClose();
				
				//notify the state change
				this.updateStatus();

			}
		}
	}

	/**
	 * Initialize the device state...
	 */
	private void initializeStates()
	{

		// initialize the state
		this.currentState.setState(OpenCloseState.class.getSimpleName(),
				new OpenCloseState(new CloseStateValue()));

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
	private boolean attachIASZoneCluster(IEndPoint endpoint)
	{
		// the success flag
		boolean done = false;

		// get the OnOff cluster
		IServiceCluster cluster = endpoint
				.getServiceCluster(IASZoneServer.class.getName());

		if (cluster != null)
		{
			// store the cluster
			this.iasZoneServerCluster = (IASZoneServer) cluster;

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
								IASZoneServer.ATTR_ZoneStatus_NAME, reqContext);

				// perform attribute subscription and get the accepted
				// subscription parameters for active energy
				if (acceptedParams == null)
				{
					acceptedParams = cluster.setAttributeSubscription(
							IASZoneServer.ATTR_ZoneStatus_NAME,
							new SubscriptionParameters(
									this.reportingTimeSeconds,
									this.reportingTimeSeconds, 0), reqContext);

					// debug
					this.debugSubscription(acceptedParams);
				}

			}
			catch (ApplianceException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								"Error (ApplianceException) while sending setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}
			catch (ServiceClusterException e)
			{
				this.logger
						.log(LogService.LOG_ERROR,
								"Error (ServiceClusterException) while setting subscription to the ZigBee appliance with serial: "
										+ this.theManagedAppliance.getSerial(),
								e);
			}

			done = true;
		}

		return done;
	}

	/**
	 * Provides debug feedback about parameter subscription results.
	 * 
	 * @param acceptedParams
	 */
	private void debugSubscription(ISubscriptionParameters acceptedParams)
	{
		// debug
		if (this.logger != null)
		{
			if (acceptedParams != null)
				this.logger.log(LogService.LOG_DEBUG, "Subscription result:"
						+ acceptedParams.getMinReportingInterval() + ","
						+ acceptedParams.getMaxReportingInterval() + ","
						+ acceptedParams.getReportableChange());
			else
				this.logger.log(LogService.LOG_DEBUG,
						"Subscripion not accepted");
		}
	}

	@Override
	public DeviceStatus getState()
	{
		return currentState;
	}

	@Override
	public void notifyOpen()
	{
		// update the state
		OpenCloseState openState = new OpenCloseState(new OpenStateValue());
		currentState.setState(OpenCloseState.class.getSimpleName(), openState);

		logger.log(
				LogService.LOG_DEBUG,
				"Device "
						+ device.getDeviceId()
						+ " is now "
						+ ((OpenCloseState) openState).getCurrentStateValue()[0]
								.getValue());

		((DoorSensor) device).notifyOpen();

	}

	@Override
	public void notifyClose()
	{
		// update the state
		OpenCloseState closeState = new OpenCloseState(new CloseStateValue());
		currentState.setState(OpenCloseState.class.getSimpleName(), closeState);

		logger.log(
				LogService.LOG_DEBUG,
				"Device "
						+ device.getDeviceId()
						+ " is now "
						+ ((OpenCloseState) closeState).getCurrentStateValue()[0]
								.getValue());

		((DoorSensor) device).notifyClose();

	}

	@Override
	public IServiceCluster[] getServiceClusters()
	{
		return this.exportedClusters;
	}

	@Override
	public void notifyApplianceAdded(IApplicationEndPoint endPoint,
			IAppliance appliance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyApplianceRemoved(IAppliance appliance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyApplianceAvailabilityUpdated(IAppliance appliance)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Provides a reference to the {@link IConnectionAdminService} currently
	 * used by this instance
	 * 
	 * @return
	 */
	public IConnectionAdminService getConnectionAdmin()
	{
		return connectionAdmin;
	}

	/**
	 * Sets the {@link IConnectionAdminService} to be used by this instance.
	 * 
	 * @param connectionAdmin
	 */
	public void setConnectionAdmin(IConnectionAdminService connectionAdmin)
	{
		this.connectionAdmin = connectionAdmin;
	}

	@Override
	public void updateStatus()
	{
		((Controllable)this.device).updateStatus();
		
	}

}
