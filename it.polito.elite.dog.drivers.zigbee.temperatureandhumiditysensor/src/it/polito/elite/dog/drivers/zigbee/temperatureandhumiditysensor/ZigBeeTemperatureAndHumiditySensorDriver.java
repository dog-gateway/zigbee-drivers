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
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.TemperatureAndHumiditySensor;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.gateway.ZigBeeGatewayDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.telecomitalia.ah.cluster.zigbee.measurement.RelativeHumidityMeasurementServer;
import it.telecomitalia.ah.cluster.zigbee.measurement.TemperatureMeasurementServer;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ZigBeeTemperatureAndHumiditySensorDriver extends ZigBeeDriver
		implements Driver, ManagedService
{
	// the bundle context
	private BundleContext context;

	// the associated network driver
	private AtomicReference<ZigBeeNetwork> network;

	// the associated gateway driver
	private AtomicReference<ZigBeeGatewayDriver> gateway;

	// the reporting time for onoff devices
	private int reportingTimeSeconds = 5; // default 5s

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// the list of instances controlled / spawned by this driver
	private HashSet<ZigBeeTemperatureAndHumiditySensorDriverInstance> managedInstances;

	// the bundle logger
	private LogHelper logger;

	// the bundle log id
	protected static final String logId = "[ZigBeeTemperatureAndHumiditySensorDriver]: ";

	/**
			 * 
			 */
	public ZigBeeTemperatureAndHumiditySensorDriver()
	{
		// create the needed atomic references
		this.network = new AtomicReference<ZigBeeNetwork>();

		this.gateway = new AtomicReference<ZigBeeGatewayDriver>();
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

		// initialize the lis of managed instances
		this.managedInstances = new HashSet<ZigBeeTemperatureAndHumiditySensorDriverInstance>();

		// create the driver description info
		this.driverInfo = new ZigBeeDriverInfo();
		this.driverInfo.setDriverName(context.getBundle().getSymbolicName());
		this.driverInfo.setDriverVersion(context.getBundle().getVersion()
				.toString());
		this.driverInfo.setMainDeviceClass(TemperatureAndHumiditySensor.class
				.getSimpleName());
		this.driverInfo.addServerClusters(TemperatureMeasurementServer.class.getName()
				.replaceAll("Server", ""), RelativeHumidityMeasurementServer.class.getName()
				.replaceAll("Server", ""));

		this.logger
				.log(LogService.LOG_DEBUG,
						ZigBeeTemperatureAndHumiditySensorDriver.logId
								+ "Activated...");
	}

	/**
	 * Deactivate the bundle removing published services in a clean way.
	 */
	public void deactivate()
	{
		// log deactivation
		this.logger.log(LogService.LOG_DEBUG,
				ZigBeeTemperatureAndHumiditySensorDriver.logId
						+ " Deactivation required");

		// remove the managed instances from the network driver
		for (ZigBeeTemperatureAndHumiditySensorDriverInstance instance : this.managedInstances)
			this.network.get().removeFromNetworkDriver(instance);

		// un-register
		this.unRegisterTemperatureAndHumiditySensorDriver();

		// null inner variables
		this.context = null;
		this.logger = null;
		this.managedInstances = null;
	}

	public void addedNetworkDriver(ZigBeeNetwork network)
	{
		// log network river addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG,
					ZigBeeTemperatureAndHumiditySensorDriver.logId
							+ " Added network driver");

		// store the network driver reference
		this.network.set(network);

		// try to register the driver service
		// this.registerEnergyAndPowerMeterDriver();
	}

	public void removedNetworkDriver(ZigBeeNetwork network)
	{
		// null the network freeing the old reference for gc
		if (this.network.compareAndSet(network, null))
		{
			// unregister the services
			this.unRegisterTemperatureAndHumiditySensorDriver();

			// log network river removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeTemperatureAndHumiditySensorDriver.logId
								+ " Removed network driver");

		}
	}

	public void addedGatewayDriver(ZigBeeGatewayDriver gateway)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG,
					ZigBeeTemperatureAndHumiditySensorDriver.logId
							+ "Added gateway driver");

		// store the network driver reference
		this.gateway.set(gateway);

	}

	public void removedGatewayDriver(ZigBeeGatewayDriver gateway)
	{
		// null the network freeing the old reference for gc
		if (this.gateway.compareAndSet(gateway, null))
		{
			// unregister the services
			this.unRegisterTemperatureAndHumiditySensorDriver();
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG,
						ZigBeeTemperatureAndHumiditySensorDriver.logId
								+ "Removed gateway driver");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized int match(ServiceReference reference) throws Exception
	{

		int matchValue = Device.MATCH_NONE;
		if (this.regDriver != null)
		{
			// get the given device category
			String deviceCategory = (String) reference
					.getProperty(DeviceCostants.DEVICE_CATEGORY);

			// get the given device manufacturer
			String manifacturer = (String) reference
					.getProperty(DeviceCostants.MANUFACTURER);

			// compute the matching score between the given device and this
			// driver
			if (deviceCategory != null)
			{
				if (manifacturer != null
						&& (manifacturer.equals(ZigBeeInfo.MANUFACTURER))
						&& (deviceCategory.equals(TemperatureAndHumiditySensor.class
								.getName())))
				{
					matchValue = TemperatureAndHumiditySensor.MATCH_MANUFACTURER
							+ TemperatureAndHumiditySensor.MATCH_TYPE;
				}

			}
		}
		return matchValue;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public synchronized String attach(ServiceReference reference)
			throws Exception
	{
		if (this.regDriver != null)
		{
			// get the controllable device to attach
			ControllableDevice device = (ControllableDevice) this.context
					.getService(reference);

			// create a new driver instance
			ZigBeeTemperatureAndHumiditySensorDriverInstance driverInstance = new ZigBeeTemperatureAndHumiditySensorDriverInstance(
					network.get(), device, this.context,
					this.reportingTimeSeconds);

			// associate device and driver
			device.setDriver(driverInstance);
		}
		// must always return null
		return null;
	}

	@Override
	public void updated(Dictionary<String, ?> configParams)
			throws ConfigurationException
	{
		if (configParams != null)
		{
			// used to store polling policies
			this.reportingTimeSeconds = Integer.valueOf((String) configParams
					.get("reportingTimeSeconds"));

			// debug
			this.logger.log(LogService.LOG_DEBUG,
					ZigBeeTemperatureAndHumiditySensorDriver.logId
							+ " Reporting time = " + this.reportingTimeSeconds
							+ "s");

			// try to register the service
			this.registerTemperatureAndHumiditySensorDriver();
		}
	}

	/**
	 * Register this bundle as an OnOff device driver
	 */
	private void registerTemperatureAndHumiditySensorDriver()
	{
		if ((this.context != null) && (this.regDriver == null)
				&& (this.network.get() != null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();

			// add the id of this driver to the properties
			propDriver.put(DeviceCostants.DRIVER_ID, this.getClass().getName());

			// register this driver in the OSGi framework
			this.regDriver = this.context.registerService(
					Driver.class.getName(), this, propDriver);

			// register the driver capability on the gateway
			this.gateway.get().addActiveDriverDetails(this.driverInfo);
		}

	}

	/**
	 * Unregisters this driver from the OSGi framework...
	 */
	private void unRegisterTemperatureAndHumiditySensorDriver()
	{
		// TODO DETACH allocated Drivers
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}

	}
}
