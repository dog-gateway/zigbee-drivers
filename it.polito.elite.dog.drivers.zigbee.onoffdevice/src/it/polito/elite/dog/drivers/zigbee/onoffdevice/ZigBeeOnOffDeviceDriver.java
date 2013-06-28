/*
 * Dog 2.0 - ZigBee OnOffDevice Driver
 * 
 * Copyright [2013] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zigbee.onoffdevice;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.domotics.dog2.doglibrary.DogDeviceCostants;
import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.polito.elite.domotics.model.devicecategory.Lamp;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

/**
 * The onoff device driver which handles OnOffDevices, on ZigBee networks.
 * 
 * @author bonino
 * 
 */
public class ZigBeeOnOffDeviceDriver implements Driver, ManagedService
{
	// the bundle context
	private BundleContext context;
	
	// the associated network driver
	private ZigBeeNetwork network;
	
	// the reporting time for onoff devices
	private int reportingTime;
	
	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;
	
	// what are the on/off device categories that can match with this driver?
	private Set<String> OnOffDeviceCategories;
	
	// //the list of instances controlled / spawned by this driver
	private HashSet<ZigBeeOnOffDeviceDriverInstance> managedInstances;
	
	// the bundle logger
	private LogService logger;
	
	// the bundle log id
	protected static final String logId = "[ZigBeeOnOffDeviceDriver]: ";
	
	/**
	 * 
	 */
	public ZigBeeOnOffDeviceDriver()
	{
		// intentionally left empty
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
		this.logger = new DogLogInstance(context);
		
		// initialize data structures
		this.OnOffDeviceCategories = new HashSet<String>();
		
		// initialize the set of managed instances
		this.managedInstances = new HashSet<ZigBeeOnOffDeviceDriverInstance>();
		
		// fill the categories
		properFillDeviceCategories();
		
		this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + "Activated...");
		
		// try to register the service
		this.registerOnOffDeviceDriver();
	}
	
	/**
	 * Deactivate the bundle removing published services in a clean way.
	 */
	public void deactivate()
	{
		// log deactivation
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + " Deactivation required");
		
		// remove the managed instances from the network driver
		for (ZigBeeOnOffDeviceDriverInstance instance : this.managedInstances)
			this.network.removeFromNetworkDriver(instance);
		
		this.unRegisterOnOffDeviceDriver();
		
		// null inner variables
		this.context = null;
		this.network = null;
		this.logger = null;
		this.managedInstances = null;
	}
	
	public void addedNetworkDriver(ZigBeeNetwork network)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + "Added network driver");
		
		// store the network driver reference
		this.network = network;
		
		// try to register the driver service
		this.registerOnOffDeviceDriver();
	}
	
	public void removedNetworkDriver(ZigBeeNetwork network)
	{
		// log network driver removal
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + "Removed network driver");
		
		// unregister the services
		this.unRegisterOnOffDeviceDriver();
		
		// null the network freeing the old reference for gc
		this.network = null;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;
		
		if (this.regDriver != null)
		{
			// get the given device category
			String deviceCategory = (String) reference.getProperty(DogDeviceCostants.DEVICE_CATEGORY);
			
			// get the given device manufacturer
			String manifacturer = (String) reference.getProperty(DogDeviceCostants.MANUFACTURER);
			
			// compute the matching score between the given device and this
			// driver
			if (deviceCategory != null)
			{
				if (manifacturer != null && (manifacturer.equals(ZigBeeInfo.MANUFACTURER))
						&& (OnOffDeviceCategories.contains(deviceCategory)))
				{
					matchValue = Lamp.MATCH_MANUFACTURER + Lamp.MATCH_TYPE;
				}
				
			}
		}
		return matchValue;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		if (this.regDriver != null)
		{
			// create a new driver instance
			ZigBeeOnOffDeviceDriverInstance driverInstance = new ZigBeeOnOffDeviceDriverInstance(network,
					(ControllableDevice) this.context.getService(reference), this.context);
			
			// associate device and driver
			((ControllableDevice) context.getService(reference)).setDriver(driverInstance);
			
		}
		
		// must always return null
		return null;
	}
	
	@Override
	public void updated(Dictionary<String, ?> configParams) throws ConfigurationException
	{
		if (configParams != null)
		{
			// used to store polling policies
			this.reportingTime = (Integer) configParams.get("reportingTimeSeconds");
			
			// debug
			this.logger.log(LogService.LOG_DEBUG, ZigBeeOnOffDeviceDriver.logId + " Reporting time = "
					+ this.reportingTime + "s");
		}
	}
	
	/**
	 * Fill a set with all the device categories whose devices can match with
	 * this driver. Automatically retrieve the device categories list by reading
	 * the implemented interfaces of its DeviceDriverInstance class bundle.
	 */
	private void properFillDeviceCategories()
	{
		for (Class<?> devCat : ZigBeeOnOffDeviceDriverInstance.class.getInterfaces())
		{
			this.OnOffDeviceCategories.add(devCat.getName());
		}
	}
	
	/**
	 * Register this bundle as an OnOff device driver
	 */
	private void registerOnOffDeviceDriver()
	{
		if ((this.context != null) && (this.regDriver == null) && (this.network != null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			
			// add the id of this driver to the properties
			propDriver.put(DogDeviceCostants.DRIVER_ID, this.getClass().getName());
			
			// register this driver in the OSGi framework
			this.regDriver = this.context.registerService(Driver.class.getName(), this, propDriver);
		}
		
	}
	
	/**
	 * Unregisters this driver from the OSGi framework...
	 */
	private void unRegisterOnOffDeviceDriver()
	{
		// TODO DETACH allocated Drivers
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}
		
	}
	
}
