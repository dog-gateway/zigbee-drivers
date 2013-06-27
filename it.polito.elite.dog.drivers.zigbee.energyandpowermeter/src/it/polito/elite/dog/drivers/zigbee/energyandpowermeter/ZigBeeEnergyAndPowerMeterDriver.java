/*
 * Dog 2.0 - ZigBee EnergyAndPowerMeter Driver
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
package it.polito.elite.dog.drivers.zigbee.energyandpowermeter;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;
import it.polito.elite.domotics.dog2.doglibrary.DogDeviceCostants;
import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.polito.elite.domotics.model.devicecategory.EnergyAndPowerMeter;

import java.util.Dictionary;
import java.util.Hashtable;

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
public class ZigBeeEnergyAndPowerMeterDriver implements Driver, ManagedService
{
	// the bundle context
		private BundleContext context;
		
		// the associated network driver
		private ZigBeeNetwork network;
		
		// the reporting time for onoff devices
		private int reportingTimeSeconds = 5; // default 5s
		
		// the registration object needed to handle the life span of this bundle in
		// the OSGi framework (it is a ServiceRegistration object for use by the
		// bundle registering the service to update the service's properties or to
		// unregister the service).
		private ServiceRegistration<?> regDriver;
		
		// the bundle logger
		private LogService logger;
		
		// the bundle log id
		protected static final String logId = "[ZigBeeEnergyAndPowerMeterDriver]: ";
		
		/**
			 * 
			 */
		public ZigBeeEnergyAndPowerMeterDriver()
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
			
			this.logger.log(LogService.LOG_DEBUG, ZigBeeEnergyAndPowerMeterDriver.logId + "Activated...");
		}
		
		/**
		 * Deactivate the bundle removing published services in a clean way.
		 */
		public void deactivate()
		{
			this.unRegisterMeteringPowerOutletDriver();
		}
		
		public void addedNetworkDriver(ZigBeeNetwork network)
		{
			// store the network driver reference
			this.network = network;
			
			// try to register the driver service
			this.registerMeteringPowerOutletDriver();
		}
		
		public void removedNetworkDriver(ZigBeeNetwork network)
		{
			// unregister the services
			this.unRegisterMeteringPowerOutletDriver();
			
			// null the network freeing the old reference for gc
			// this.network = null;
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public int match(ServiceReference reference) throws Exception
		{
			int matchValue = Device.MATCH_NONE;
			
			// get the given device category
			String deviceCategory = (String) reference.getProperty(DogDeviceCostants.DEVICE_CATEGORY);
			
			// get the given device manufacturer
			String manifacturer = (String) reference.getProperty(DogDeviceCostants.MANUFACTURER);
			
			// compute the matching score between the given device and this driver
			if (deviceCategory != null)
			{
				if (manifacturer != null && (manifacturer.equals(ZigBeeInfo.MANUFACTURER))
						&& (deviceCategory.equals(EnergyAndPowerMeter.class.getName())))
				{
					matchValue = EnergyAndPowerMeter.MATCH_MANUFACTURER + EnergyAndPowerMeter.MATCH_TYPE;
				}
				
			}
			return matchValue;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public String attach(ServiceReference reference) throws Exception
		{
			// create a new driver instance
			ZigBeeEnergyAndPowerMeterDriverInstance driverInstance = new ZigBeeEnergyAndPowerMeterDriverInstance(network,
					(ControllableDevice) this.context.getService(reference), this.context, this.reportingTimeSeconds);
			
			// associate device and driver
			((ControllableDevice) context.getService(reference)).setDriver(driverInstance);
			
			// must always return null
			return null;
		}
		
		@Override
		public void updated(Dictionary<String, ?> configParams) throws ConfigurationException
		{
			if(configParams != null)
			{
				// used to store polling policies
				this.reportingTimeSeconds = Integer.valueOf((String) configParams.get("reportingTimeSeconds"));
				
				// debug
				this.logger.log(LogService.LOG_DEBUG, ZigBeeEnergyAndPowerMeterDriver.logId + " Reporting time = "
						+ this.reportingTimeSeconds + "s");
				
				// try to register the service
				this.registerMeteringPowerOutletDriver();
			}
		}
		
		/**
		 * Register this bundle as an OnOff device driver
		 */
		private void registerMeteringPowerOutletDriver()
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
		private void unRegisterMeteringPowerOutletDriver()
		{
			// TODO DETACH allocated Drivers
			if (this.regDriver != null)
			{
				this.regDriver.unregister();
				this.regDriver = null;
			}
			
		}
}
