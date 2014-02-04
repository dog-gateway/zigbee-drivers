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
package it.polito.elite.dog.drivers.zigbee.meterbridge;

import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.EnergyAndPowerMeter;
import it.polito.elite.dog.core.library.model.devicecategory.MeteringPowerOutlet;
import it.polito.elite.dog.core.library.model.devicecategory.SinglePhaseElectricityMeter;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.meterbridge.appliance.BridgedMeteringAppliance;
import it.polito.elite.dog.drivers.zigbee.meterbridge.cluster.MeterBridgeSimpleMeteringServerCluster;
import it.polito.elite.dog.drivers.zigbee.meterbridge.endpoint.MeterBridgeEndpoint;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.hac.ApplianceException;
import it.telecomitalia.ah.hac.IAppliance;
import it.telecomitalia.ah.hac.IManagedAppliance;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Constants;
import org.osgi.service.device.Device;
import org.osgi.service.log.LogService;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This class creates virtual appliances for bridging sensors belonging to other
 * networks to a ZigBee network. Currently, supports only single phase metering devices are supported, i.e.,
 * {@link MeteringPowerOutlet} and {@link EnergyAndPowerMeter}.
 * 
 * @author bonino
 * 
 */
public class MeterBridge implements ManagedService,
		ServiceTrackerCustomizer<Object, Object>
{
	// the reference to the network driver, only used to establish a dependancy
	// on the existence of a ZigBee network
	private AtomicReference<ZigBeeNetwork> network;

	// the reference to the monitor admin, used to query the current state of
	// devices to bridge
	private AtomicReference<MonitorAdmin> monitorAdmin;

	// the bundle context used to register services and get access to services
	// provided by other bundles
	private BundleContext context;

	// the device filter for listening to device creation
	private Filter deviceImplFilter;

	// the bundle logger
	private LogHelper logger;

	// the configuration key
	private static final String METERS_TO_BRIDGE = "metersToBridge";

	// the set of devices waiting to be bridged
	private HashSet<String> devicesToBridge;

	// the set of bridged devices
	private Hashtable<String, BridgedMeteringAppliance> bridgedDevices;

	// the set of service registrations for bridged devices
	private Hashtable<String, ServiceRegistration<?>> bridgedDevicesRegistrations;

	// the set of supported device categories
	private Set<String> supportedCategories;

	/**
	 * Initializes the inner datas structures
	 */
	public MeterBridge()
	{
		// initialize the set of devices to bridge
		this.devicesToBridge = new HashSet<String>();

		// initialize the bridged devices
		this.bridgedDevices = new Hashtable<String, BridgedMeteringAppliance>();

		// initialize the bridged devices registrations
		this.bridgedDevicesRegistrations = new Hashtable<String, ServiceRegistration<?>>();

		// initialize the atomic references
		this.network = new AtomicReference<ZigBeeNetwork>();
		this.monitorAdmin = new AtomicReference<MonitorAdmin>();

		// initialize the supported categories
		this.supportedCategories = new HashSet<String>();

		// store supported categories
		this.supportedCategories.add(SinglePhaseElectricityMeter.class
				.getName());
		this.supportedCategories.add(MeteringPowerOutlet.class.getName());
		this.supportedCategories.add(EnergyAndPowerMeter.class.getName());
	}

	/**
	 * Activate the bundle
	 * 
	 * @param context
	 */
	public void activate(BundleContext context)
	{
		// store the context
		this.context = context;

		// initialize the bundle logger
		this.logger = new LogHelper(context);

		this.logger.log(LogService.LOG_DEBUG, "Activated...");

		// track devices
		String[] filterData = new String[] {
				org.osgi.framework.Constants.OBJECTCLASS,
				Device.class.getName(),
				org.osgi.framework.Constants.OBJECTCLASS, "*",
				Constants.DEVICE_CATEGORY, "*" };

		try
		{
			this.deviceImplFilter = FrameworkUtil.createFilter(String.format(
					"(|(%s=%s)(&(%s=%s)(%s=%s)))", (Object[]) filterData));
		}
		catch (InvalidSyntaxException e)
		{
			this.logger.log(LogService.LOG_ERROR, "Unable to track devices...",
					e);
		}
	}

	/**
	 * Deactivate the bundle removing published services in a clean way.
	 */
	public void deactivate()
	{
		// log deactivation
		this.logger.log(LogService.LOG_DEBUG, " Deactivation required");

		// null inner variables
		this.context = null;
		this.logger = null;

	}

	/**
	 * Handle binding of network driver (mainly used to set a dependency on the existence of a ZigBee network).
	 * @param network
	 */
	public void addedNetworkDriver(ZigBeeNetwork network)
	{
		// log network river addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, " Added network driver");

		// store the network driver reference
		this.network.set(network);

	}

	/**
	 * Handle removal of network driver.
	 * @param network
	 */
	public void removedNetworkDriver(ZigBeeNetwork network)
	{
		// null the network freeing the old reference for gc
		if (this.network.compareAndSet(network, null))
		{

			// log network river removal
			if (this.logger != null)
				this.logger
						.log(LogService.LOG_DEBUG, " Removed network driver");

		}
	}

	/**
	 * Handle binding of {@link MonitorAdmin} for handling meter state bridging
	 * @param monitorAdmin
	 */
	public void addedMonitorAdmin(MonitorAdmin monitorAdmin)
	{
		// log network river addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, " Added monitor admin");

		// store the network driver reference
		this.monitorAdmin.set(monitorAdmin);

	}

	/**
	 * Handle un-binding of {@link MonitorAdmin}
	 * @param monitorAdmin
	 */
	public void removedMonitorAdmin(MonitorAdmin monitorAdmin)
	{
		// null the network freeing the old reference for gc
		if (this.monitorAdmin.compareAndSet(monitorAdmin, null))
		{

			// log network river removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG, " Removed monitor admin");

		}
	}

	@Override
	public void updated(Dictionary<String, ?> configuration)
			throws ConfigurationException
	{
		// check not null
		if (configuration != null)
		{
			// parse devices to bridge (comma separated)
			String devicesToBridgeUnparsed = (String) configuration
					.get(MeterBridge.METERS_TO_BRIDGE);

			// check not null or empty
			if ((devicesToBridgeUnparsed != null)
					&& (!devicesToBridgeUnparsed.isEmpty()))
			{
				// split and trim
				String devicesToBridge[] = devicesToBridgeUnparsed.split(",");

				// store the device to be bridged
				Collections.addAll(this.devicesToBridge, devicesToBridge);

				// start tracking devices
				ServiceTracker<Object, Object> devTracker = new ServiceTracker<Object, Object>(
						context, this.deviceImplFilter, this);
				devTracker.open();
			}
		}

	}

	/**
	 * Creates a virtual brdige appliance for the given device service having the give device id.
	 * @param deviceService The device service to bridge.
	 * @param deviceId The device unique id (URI).
	 */
	private void bridgeDevice(ServiceReference<?> deviceService, String deviceId)
	{

		// if the device exist, than it can be bridged...
		if (deviceService != null)
		{

			// check if the device is a single phase meter
			String deviceCategory = (String) deviceService
					.getProperty(DeviceCostants.DEVICE_CATEGORY);

			if (this.supportedCategories.contains(deviceCategory))
			{
				// create the bridged appliance
				try
				{
					// prepare the appliance configuration parameters
					Hashtable<String, Object> config = new Hashtable<String, Object>();
					config.put(IAppliance.APPLIANCE_NAME_PROPERTY,
							"ah.app.meterbridge." + deviceId);

					// create the bridged appliance
					BridgedMeteringAppliance appliance = new BridgedMeteringAppliance(
							"ah.app.meterbridge." + deviceId, config);

					// create the needed endpoint
					MeterBridgeEndpoint endpoint = new MeterBridgeEndpoint(
							"ah.app.meterbridge", appliance);

					// create the OnOffServer cluster which will handle
					// OnOffClient
					// requests
					MeterBridgeSimpleMeteringServerCluster cluster = new MeterBridgeSimpleMeteringServerCluster(
							this.monitorAdmin.get(), deviceId, appliance,
							this.logger);

					// add the cluster to the endpoint
					endpoint.addServiceCluster(cluster);

					// add the endpoint to the appliance
					appliance.addEndPoint(endpoint);

					// store the appliance
					this.bridgedDevices.put(deviceId, appliance);

					// flag the appliance as available
					appliance.setAvailability(true);

					// register the appliance
					this.bridgedDevicesRegistrations.put(deviceId, this.context
							.registerService(IManagedAppliance.class.getName(),
									appliance, null));

				}
				catch (ApplianceException e)
				{
					// log the error
					this.logger.log(LogService.LOG_WARNING,
							"Unable to bridge device " + deviceId, e);
				}
			}
		}

	}

	@Override
	public Object addingService(ServiceReference<Object> reference)
	{
		// log the device detection
		this.logger.log(
				LogService.LOG_INFO,
				"Detected new device "
						+ reference.getProperty(DeviceCostants.DEVICEURI));

		String deviceId = (String) reference
				.getProperty(DeviceCostants.DEVICEURI);
		if ((deviceId != null) && (this.devicesToBridge.contains(deviceId)))
		{
			// bridge the device
			this.bridgeDevice(reference, deviceId);

			// remove the device from the list of devices waiting for being
			// bridged
			this.devicesToBridge.remove(deviceId);
		}

		return null;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference,
			Object service)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removedService(ServiceReference<Object> reference,
			Object service)
	{

		String deviceId = (String) reference
				.getProperty(DeviceCostants.DEVICEURI);
		if ((deviceId != null) && (this.bridgedDevices.contains(deviceId)))
		{
			// remove the device
			this.bridgedDevices.remove(deviceId);

			// get the bridged device registration
			ServiceRegistration<?> deviceReg = this.bridgedDevicesRegistrations
					.get(deviceId);

			// unregister
			deviceReg.unregister();

			// remove the device registration
			this.bridgedDevicesRegistrations.remove(deviceId);

			// add the device to the list of devices waiting for being
			// bridged
			this.devicesToBridge.add(deviceId);
		}

	}

}
