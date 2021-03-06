/*
 * Dog 2.0 - ZigBee Network Driver
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
package it.polito.elite.dog.drivers.zigbee.network;

import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ApplianceDiscoveryListener;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import org.energy_home.jemma.ah.hac.IAppliance;
import org.energy_home.jemma.ah.hac.IApplicationEndPoint;
import org.energy_home.jemma.ah.hac.IApplicationService;
import org.energy_home.jemma.ah.hac.IAttributeValue;
import org.energy_home.jemma.ah.hac.IAttributeValuesListener;
import org.energy_home.jemma.ah.hac.IServiceCluster;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

/**
 * The network driver for devices based on the ZigBee network protocol
 * 
 * @author bonino
 * 
 */
public class ZigBeeNetworkDriver implements IApplicationService,
		IAttributeValuesListener, ZigBeeNetwork, ManagedService
{
	// the bundle logger
	private LogHelper logger;

	// the bundle context
	private BundleContext context;

	// the log id
	private static final String logId = "[ZigBeeNetworkDriver]: ";

	// the appliance / endpoint map
	private ConcurrentHashMap<String, ZigBeeApplianceInfo> connectedAppliances;

	// the serial/driver map
	private ConcurrentHashMap<String, ZigBeeDriverInstance> connectedDrivers;

	// the registered appliance discovery listeners
	private HashSet<ApplianceDiscoveryListener> discoveryListeners;

	// the service registration handle
	private ServiceRegistration<?> regServiceZigBeeNetwork;

	/**
	 * Creates an instance of {@link ZigBeeNetworkDriver}, typically called
	 * before activation.
	 */
	public ZigBeeNetworkDriver()
	{
		// init data structures
		this.connectedAppliances = new ConcurrentHashMap<String, ZigBeeApplianceInfo>();
		this.connectedDrivers = new ConcurrentHashMap<String, ZigBeeDriverInstance>();
		this.discoveryListeners = new HashSet<ApplianceDiscoveryListener>();
	}

	public void activate(BundleContext context)
	{
		// store the bundle context
		this.context = context;

		// initialize the class logger...
		this.logger = new LogHelper(context);

		// debug: signal activation...
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId
				+ "Activated...");

		// register the service
		this.registerNetworkService();
	}

	public void deactivate()
	{
		// unregister the service
		this.unregisterNetworkService();

		// log
		this.logger.log(LogService.LOG_INFO, ZigBeeNetworkDriver.logId
				+ "Deactivated...");
	}

	@Override
	public IServiceCluster[] getServiceClusters()
	{
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId
				+ "Get Service clusters");
		return null;
	}

	@Override
	public void notifyApplianceAdded(IApplicationEndPoint endpoint,
			IAppliance appliance)
	{

		// create a ZigBeeApplianceInfo object representing the appliance
		ZigBeeApplianceInfo applianceInfo = new ZigBeeApplianceInfo(endpoint,
				appliance);

		// store the appliance info
		this.connectedAppliances.put(applianceInfo.getSerial(), applianceInfo);

		// update any already connected drivers and if no driver is currently
		// bound to the appliance, trigger appliance discovery
		String serial = applianceInfo.getSerial();
		if (!this.updateApplianceDriverBinding(serial))
			this.triggerApplianceDiscovery(serial);

		// debug
		this.logger.log(LogService.LOG_INFO, ZigBeeNetworkDriver.logId
				+ "Added appliance: \n" + applianceInfo);

	}

	@Override
	public void notifyApplianceRemoved(IAppliance appliance)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId
				+ "Appliance removed: "
				+ appliance.getDescriptor().getFriendlyName());

		// TODO: update bound drivers (e.g., by removing the bound appliance
		// info)

	}

	@Override
	public void notifyApplianceAvailabilityUpdated(IAppliance appliance)
	{
		// get the appliance serial number
		String serial = ZigBeeApplianceInfo.extractApplianceSerial(appliance);

		// get the appliance from the set of connected appliances
		ZigBeeApplianceInfo applianceInfo = this.connectedAppliances
				.get(serial);

		// check not null
		if (applianceInfo != null)
		{
			// update the appliance reference...
			applianceInfo.setAppliance(appliance);

			// update the driver appliance binding
			this.updateApplianceDriverBinding(serial);

			// debug
			this.logger.log(LogService.LOG_INFO, ZigBeeNetworkDriver.logId
					+ "Updated appliance: \n" + applianceInfo);
		}

	}

	@Override
	public IAppliance addToNetworkDriver(String applianceSerial,
			ZigBeeDriverInstance driver)
	{
		// add to the map of connected drivers
		this.connectedDrivers.put(applianceSerial, driver);

		// check if the appliance already exists and if such, call back the
		// driver method...
		// this.updateApplianceDriverBinding(applianceSerial);

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork#
	 * removeFromNetworkDriver
	 * (it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver)
	 */
	@Override
	public void removeFromNetworkDriver(ZigBeeDriverInstance driver)
	{
		HashSet<String> toRemove = new HashSet<String>();

		// get the connected drivers
		for (Entry<String, ZigBeeDriverInstance> entry : this.connectedDrivers
				.entrySet())
		{
			// get the key, if any
			if (entry.getValue().equals(driver))
				toRemove.add(entry.getKey());
		}

		// remove the corresponding entries
		for (String key : toRemove)
		{
			this.connectedDrivers.remove(key);
		}

	}

	/**
	 * Register this bundle as network driver
	 */
	private void registerNetworkService()
	{
		if (this.regServiceZigBeeNetwork == null)
			this.regServiceZigBeeNetwork = this.context.registerService(
					ZigBeeNetwork.class.getName(), this, null);

	}

	/**
	 * Unregister this bundle
	 */
	private void unregisterNetworkService()
	{

		if (this.regServiceZigBeeNetwork != null)
		{
			this.regServiceZigBeeNetwork.unregister();
		}
	}

	/**
	 * Updates the existing bindings between appliance and drivers, if the
	 * appliance is new, trigger new appliance detection
	 * 
	 * @param applianceSerial
	 */
	private synchronized boolean updateApplianceDriverBinding(
			String applianceSerial)
	{
		// the update status
		boolean updated = false;

		// get the associated appliance info, if any
		ZigBeeApplianceInfo applianceInfo = this.connectedAppliances
				.get(applianceSerial);

		// get the associated driver, if any
		ZigBeeDriverInstance driver = this.connectedDrivers.get(applianceSerial);

		// notify the driver if an appliance is available with the given serial
		// number
		if (applianceInfo != null)
		{
			if (driver != null)
			{
				// update the driver binding
				driver.setIAppliance(applianceInfo);

				// report successful update
				updated = true;
			}
		}

		return updated;

	}

	/**
	 * Triggers appliance discovery on the low level ZigBee device having the
	 * given serial number
	 * 
	 * @param applianceSerial The serial of the ZigBee device to discover.
	 */
	private synchronized void triggerApplianceDiscovery(String applianceSerial)
	{
		// get the associated appliance info, if any
		ZigBeeApplianceInfo applianceInfo = this.connectedAppliances
				.get(applianceSerial);
		// notify new appliance listeners
		// TODO: check if this should be performed in a worker thread
		for (ApplianceDiscoveryListener listener : this.discoveryListeners)
		{
			listener.applianceDiscovered(applianceInfo);
		}
	}

	@Override
	public ZigBeeApplianceInfo getZigBeeApplianceInfo(String applianceSerial)
	{
		return this.connectedAppliances.get(applianceSerial);
	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId
				+ "Updated configuration...");

		// register the service?

		// left for future uses...
	}

	@Override
	public void notifyAttributeValue(String appliancePid, Integer endPointId,
			String clusterName, String attributeName,
			IAttributeValue attributeValue)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, ZigBeeNetworkDriver.logId
				+ "Received event from " + appliancePid + " name: "
				+ attributeName + " value:" + attributeValue);

		// notify to the device-specific driver
		String serial = ZigBeeApplianceInfo
				.extractApplianceSerial(appliancePid);
		if ((serial != null) && (this.connectedDrivers.containsKey(serial)))
		{
			this.connectedDrivers.get(serial).newMessageFromHouse(endPointId,
					clusterName, attributeName, attributeValue);
		}
	}

	@Override
	public void addApplianceDiscoveryListener(
			ApplianceDiscoveryListener listener)
	{
		synchronized (this.discoveryListeners)
		{
			this.discoveryListeners.add(listener);
		}

	}

	@Override
	public void removeApplianceDiscoveryListener(
			ApplianceDiscoveryListener listener)
	{
		synchronized (this.discoveryListeners)
		{
			this.discoveryListeners.remove(listener);
		}

	}

}
