/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.device;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zigbee.gateway.ZigBeeGatewayDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriver;
import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeInfo;
import it.polito.elite.dog.drivers.zigbee.network.interfaces.ZigBeeNetwork;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
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
public abstract class ZigBeeDeviceDriver extends ZigBeeDriver implements
		Driver, ManagedService
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
	protected Hashtable<String, ZigBeeDriverInstance> managedInstances;

	// the bundle logger
	protected LogHelper logger;

	// the bundle log id
	protected String logId = "[ZigBeeLightSensorDriver]: ";

	// the set of supported server clusters
	protected Set<String> serverClusters;

	// the set of supported client clusters
	protected Set<String> clientClusters;

	// the device glass used for auto-configuration
	protected String deviceMainClass;

	protected Set<String> deviceCategories;

	/**
				 * 
				 */
	public ZigBeeDeviceDriver()
	{
		// create the needed atomic references
		this.network = new AtomicReference<ZigBeeNetwork>();

		this.gateway = new AtomicReference<ZigBeeGatewayDriver>();

		// create the set of device categories
		this.deviceCategories = new HashSet<String>();

		// create the set of clusters to expose
		this.clientClusters = new HashSet<String>();
		this.serverClusters = new HashSet<String>();

		// initialize the lis of managed instances
		this.managedInstances = new Hashtable<String, ZigBeeDriverInstance>();

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

		// create the driver description info
		this.driverInfo = new ZigBeeDriverInfo();
		this.driverInfo.setDriverName(context.getBundle().getSymbolicName());
		this.driverInfo.setDriverVersion(context.getBundle().getVersion()
				.toString());
		this.driverInfo.setMainDeviceClass(this.deviceMainClass);
		this.driverInfo.addServerClusters(this.serverClusters);
		this.driverInfo.addClientClusters(this.clientClusters);

		this.logger.log(LogService.LOG_DEBUG, this.logId + "Activated...");
	}

	/**
	 * Deactivate the bundle removing published services in a clean way.
	 */
	public void deactivate()
	{
		// log deactivation
		this.logger.log(LogService.LOG_DEBUG, this.logId
				+ " Deactivation required");

		// remove the managed instances from the network driver
		for (ZigBeeDriverInstance instance : this.managedInstances.values())
			this.network.get().removeFromNetworkDriver(instance);

		// un-register
		this.unRegisterZigBeeDeviceDriver();

		// null inner variables
		this.context = null;
		this.logger = null;
		this.managedInstances = null;
	}

	public void addedNetworkDriver(ZigBeeNetwork network)
	{
		// log network river addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, this.logId
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
			this.unRegisterZigBeeDeviceDriver();

			// log network river removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG, this.logId
						+ " Removed network driver");

		}
	}

	public void addedGatewayDriver(ZigBeeGatewayDriver gateway)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, this.logId
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
			this.unRegisterZigBeeDeviceDriver();
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG, this.logId
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
			String manufacturer = (String) reference
					.getProperty(DeviceCostants.MANUFACTURER);

			// compute the matching score between the given device and this
			// driver
			if (deviceCategory != null)
			{
				if (manufacturer != null
						&& (manufacturer.equals(ZigBeeInfo.MANUFACTURER))
						&& (this.deviceCategories.contains(deviceCategory)))
				{
					matchValue = Controllable.MATCH_MANUFACTURER
							+ Controllable.MATCH_TYPE;
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

			// check if not already attached
			if (!this.managedInstances.containsKey(device.getDeviceId()))
			{

				// create a new driver instance
				ZigBeeDriverInstance driverInstance = this
						.createNewZigBeeDriverInstance(network.get(), device,
								this.context, this.reportingTimeSeconds);

				// associate device and driver
				device.setDriver(driverInstance);

				// store the managed instances
				this.managedInstances.put(device.getDeviceId(), driverInstance);
			}
		}
		// must always return null
		return null;
	}

	public abstract ZigBeeDriverInstance createNewZigBeeDriverInstance(
			ZigBeeNetwork zigBeeNetwork, ControllableDevice device,
			BundleContext context, int reportingTimeSeconds);

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
			this.logger.log(LogService.LOG_DEBUG, this.logId
					+ " Reporting time = " + this.reportingTimeSeconds + "s");

			// try to register the service
			this.registerZigBeeDeviceDriver();
		}
	}

	/**
	 * Register this bundle as an OnOff device driver
	 */
	private void registerZigBeeDeviceDriver()
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
	private void unRegisterZigBeeDeviceDriver()
	{
		// TODO DETACH allocated Drivers
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}

	}

}
