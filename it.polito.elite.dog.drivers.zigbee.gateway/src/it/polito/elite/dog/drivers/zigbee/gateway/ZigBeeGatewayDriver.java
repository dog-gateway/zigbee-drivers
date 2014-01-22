/*
 * Dog - ZigBee Gateway Driver
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
package it.polito.elite.dog.drivers.zigbee.gateway;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.ZWaveGateway;
import it.polito.elite.dog.core.library.model.devicecategory.ZigBeeGateway;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.hac.lib.ext.IAppliancesProxy;
import it.telecomitalia.ah.hac.lib.ext.INetworkManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeGatewayDriver implements Driver
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	LogHelper logger;

	// the log identifier, unique for the class
	public static final String LOG_ID = "[ZigBeeGatewayDriver]: ";

	// String identifier for driver id
	public static final String DRIVER_ID = "it.polito.elite.drivers.zigbee.gateway";

	// a reference to the network driver (currently not used by this driver
	// version, in the future might be used to implement gateway-specific
	// functionalities.
	private AtomicReference<ZigBeeNetwork> network;

	// a reference to the device factory service used to handle run-time
	// creation of devices in dog as response to network association.
	private AtomicReference<DeviceFactory> deviceFactory;

	// a reference to the HAC NetworkManager service used to handle
	// gateway-specific operations.
	private AtomicReference<INetworkManager> hacNetworkManager;

	// a reference to the HAC AppliancesProxy service used to handle
	// gateway-specific operations.
	private AtomicReference<IAppliancesProxy> hacAppliancesProxy;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// register this driver as a gateway used by device-specific drivers
	// not needed in this version as the ZigBee gateway is only one at time...
	// might be different in the future when more than one Gal connection will
	// be supported.
	private ServiceRegistration<?> regZigBeeGateway;

	// the set of currently connected gateways... indexed by their ids
	// for future use, now hosts only a single value
	private ConcurrentHashMap<String, ZigBeeGatewayDriverInstance> connectedGateways;

	// the dictionary containing the current snapshot of the driver
	// configuration, i.e., the list of supported devices and the corresponding
	// DogOnt types.
	private Set<ZigBeeDriverInfo> activeDriverDetails;

	public ZigBeeGatewayDriver()
	{
		// initialize the map of connected gateways
		this.connectedGateways = new ConcurrentHashMap<String, ZigBeeGatewayDriverInstance>();

		// initialize the supported devices map
		this.activeDriverDetails = Collections
				.synchronizedSet(new HashSet<ZigBeeDriverInfo>());

		// initialize the network driver reference
		this.network = new AtomicReference<ZigBeeNetwork>();

		// initialize the device factory reference
		this.deviceFactory = new AtomicReference<DeviceFactory>();

		// initialize the hac network manager
		this.hacNetworkManager = new AtomicReference<INetworkManager>();

		// initialize the hac appliances proxy
		this.hacAppliancesProxy = new AtomicReference<IAppliancesProxy>();

	}

	/**
	 * Handle the bundle activation
	 */
	public void activate(BundleContext bundleContext)
	{
		// store the context
		context = bundleContext;

		// init the logger
		logger = new LogHelper(context);
		
		registerDriver();
	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegister();
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void unRegister()
	{
		// un-registers this driver
		if (regDriver != null)
		{
			regDriver.unregister();
			regDriver = null;
		}

		// un-register the gateway service
		if (regZigBeeGateway != null)
		{
			regZigBeeGateway.unregister();
			regZigBeeGateway = null;
		}
	}

	// --------------- Handling service binding ---------------------

	/**
	 * 
	 * @param networkDriver
	 */
	public void addedNetworkDriver(ZigBeeNetwork networkDriver)
	{
		this.network.set(networkDriver);

	}

	/**
	 * 
	 * @param networkDriver
	 */
	public void removedNetworkDriver(ZigBeeNetwork networkDriver)
	{
		if (this.network.compareAndSet(networkDriver, null))
			// unregisters this driver from the OSGi framework
			unRegister();
	}

	/**
	 * 
	 * @param deviceFactory
	 */
	public void addedDeviceFactory(DeviceFactory deviceFactory)
	{
		this.deviceFactory.set(deviceFactory);

	}

	/**
	 * 
	 * @param deviceFactory
	 */
	public void removedDeviceFactory(DeviceFactory deviceFactory)
	{
		if (this.deviceFactory.compareAndSet(deviceFactory, null))
			// unregisters this driver from the OSGi framework
			unRegister();
	}

	/**
	 * 
	 * @param appliancesProxy
	 */
	public void addedAppliancesProxy(IAppliancesProxy appliancesProxy)
	{
		this.hacAppliancesProxy.set(appliancesProxy);
		;
	}

	/**
	 * 
	 * @param appliancesProxy
	 */
	public void removedAppliancesProxy(IAppliancesProxy appliancesProxy)
	{
		this.hacAppliancesProxy.compareAndSet(appliancesProxy, null);
	}

	/**
	 * 
	 * @param networkManager
	 * @param properties
	 */
	public void addedNetworkManager(INetworkManager networkManager,
			Map<String, String> properties)
	{
		String key = (String) properties.get("network.type");
		if (key == null)
			this.logger
					.log(LogService.LOG_WARNING,
							"attempt to attach an invalid network manager service, skipping...");
		else if (key.equals("ZigBee"))
		{
			this.hacNetworkManager.set(networkManager);
		}
	}

	/**
	 * 
	 * @param networkManager
	 * @param properties
	 */
	public void removedNetworkManager(INetworkManager networkManager,
			Map<String, String> properties)
	{
		String key = (String) properties.get("network.type");
		if (key == null)
			this.logger
					.log(LogService.LOG_WARNING,
							"attempt to remove a reference to an invalid network manager service, skipping...");
		else if (key.equals("ZigBee"))
		{
			this.hacNetworkManager.compareAndSet(networkManager, null);
		}
	}

	/**
	 * Registers this driver in the OSGi framework, making its services
	 * available to all the other bundles living in the same or in connected
	 * frameworks.
	 */
	private void registerDriver()
	{
		if ((network.get() != null) && (this.context != null)
				&& (this.regDriver == null))
		{
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			propDriver.put(DeviceCostants.DRIVER_ID, DRIVER_ID);
			propDriver.put(DeviceCostants.GATEWAY_COUNT,
					connectedGateways.size());

			this.regDriver = this.context.registerService(
					Driver.class.getName(), this, propDriver);
			this.regZigBeeGateway = this.context.registerService(
					ZigBeeGatewayDriver.class.getName(), this, null);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;

		// get the given device category
		String deviceCategory = (String) reference
				.getProperty(DeviceCostants.DEVICE_CATEGORY);

		// get the given device manufacturer
		String manufacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// compute the matching score between the given device and this driver
		if (deviceCategory != null)
		{
			if (manufacturer != null
					&& manufacturer.equals(ZigBeeInfo.MANUFACTURER)
					&& (deviceCategory.equals(ZigBeeGateway.class.getName())))
			{
				matchValue = ZWaveGateway.MATCH_MANUFACTURER
						+ ZWaveGateway.MATCH_TYPE;
			}

		}
		return matchValue;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		if (this.regDriver != null)
		{
			// get the controllable device to attach
			@SuppressWarnings("unchecked")
			ControllableDevice device = (ControllableDevice) this.context
					.getService(reference);

			// get the device id
			String deviceId = device.getDeviceId();

			if (!this.isGatewayAvailable(deviceId))
			{
				// create a new driver instance
				ZigBeeGatewayDriverInstance driverInstance = new ZigBeeGatewayDriverInstance(
						this.network.get(), this.deviceFactory.get(),
						this.hacAppliancesProxy.get(),
						this.hacNetworkManager.get(), device,
						this.activeDriverDetails, this.context);

				// associate device and driver
				device.setDriver(driverInstance);

				// store the just created gateway instance
				synchronized (connectedGateways)
				{
					// store a reference to the gateway driver
					connectedGateways.put(device.getDeviceId(), driverInstance);
				}

				// modify the service description causing a forcing the
				// framework to send a modified service notification
				final Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
				propDriver.put(DeviceCostants.DRIVER_ID, DRIVER_ID);
				propDriver.put(DeviceCostants.GATEWAY_COUNT,
						connectedGateways.size());

				this.regDriver.setProperties(propDriver);
			}
		}

		return null;
	}

	/**
	 * check if the gateway identified by the given gateway id is currently
	 * registered with this driver
	 * 
	 * @param gatewayId
	 * @return true if the gateway corresponding to the given id is already
	 *         registered, false otherwise.
	 */
	public boolean isGatewayAvailable(String gatewayId)
	{
		return connectedGateways.containsKey(gatewayId);
	}

	/**
	 * Returns a live reference to the specific network driver service to which
	 * this {@link ZWaveGatewayDriver} is connected.
	 * 
	 * @return
	 */
	public ZigBeeNetwork getNetwork()
	{
		return network.get();
	}

	/**
	 * Registers the capabilities of an active driver.
	 * 
	 * @param the
	 *            {@link ZigBeeDriverInfo} instance describing the driver
	 */
	public void addActiveDriverDetails(ZigBeeDriverInfo driverInfo)
	{
		this.activeDriverDetails.add(driverInfo);
		
		this.logger.log(LogService.LOG_INFO, "Added new driver details:\n"+driverInfo);
	}

	/**
	 * Unregisters the capabilities of an active driver
	 */
	public void removeActiveDriverDetails(ZigBeeDriverInfo driverInfo)
	{
		this.activeDriverDetails.remove(driverInfo);
	}
}
