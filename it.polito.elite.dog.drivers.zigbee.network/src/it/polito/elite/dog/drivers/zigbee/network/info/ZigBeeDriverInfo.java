/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.network.info;

import it.polito.elite.dog.drivers.zigbee.network.ZigBeeDriverInstance;
import it.polito.elite.dog.drivers.zigbee.network.util.Sets;
import it.telecomitalia.ah.cluster.ah.ConfigServer;
import it.telecomitalia.ah.cluster.zigbee.general.BasicServer;
import it.telecomitalia.ah.cluster.zigbee.general.IdentifyServer;
//import it.telecomitalia.ah.cluster.zigbee.general.TimeClient;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author bonino
 * 
 */
public class ZigBeeDriverInfo
{
	// the driver name
	String driverName;

	// the driver version;
	String driverVersion;

	// the "main" class
	String mainDeviceClass;

	// the appliance server Clusters
	private HashSet<String> serverClusters;

	// the appliance client Clusters
	private HashSet<String> clientClusters;
	
	// the cardinality of basic clusters
	private int basicClustersCardinality;

	/**
	 * Create a new driver description object (ZigBeeDriverInfo) attached to the
	 * given driver instance
	 * 
	 * @param driver
	 *            a {@link ZigBeeDriverInstance} instance.
	 */
	public ZigBeeDriverInfo()
	{
		// init the cluster sets
		this.clientClusters = new HashSet<String>();
		this.serverClusters = new HashSet<String>();

		// add common client clusters
		//Collections.addAll(this.clientClusters, TimeClient.class.getName()
		//		.replaceAll("Client", ""));

		// add common server clusters
		Collections.addAll(this.serverClusters, BasicServer.class.getName()
				.replaceAll("Server", ""), IdentifyServer.class.getName()
				.replaceAll("Server", ""), ConfigServer.class.getName()
				.replaceAll("Server", ""));
		
		this.basicClustersCardinality = this.clientClusters.size()+this.serverClusters.size();
	}

	/**
	 * Get the driver name (as registered in the framework, or specified by the
	 * driver itself). Please notice that the given name, if any CANNOT be
	 * reliably used for uniquely identifying the driver.
	 * 
	 * @return The driver name, if available.
	 */
	public String getDriverName()
	{
		return driverName;
	}

	/**
	 * Get the driver name (as registered in the framework, or specified by the
	 * driver itself). Please notice that the given name, if any CANNOT be
	 * reliably used for uniquely identifying the driver.
	 * 
	 * @param driverName
	 *            the driver name (as registered in the framework, or specified
	 *            by the driver itself)
	 */
	public void setDriverName(String driverName)
	{
		this.driverName = driverName;
	}

	/**
	 * Get the driver version (as registered in the framework, or specified by
	 * the driver itself). Please notice that the given name, if any CANNOT be
	 * reliably used for uniquely identifying the driver version.
	 * 
	 * @return The driver version, if any.
	 */
	public String getDriverVersion()
	{
		return driverVersion;
	}

	/**
	 * Set the driver version (as registered in the framework, or specified by
	 * the driver itself). Please notice that the given name, if any CANNOT be
	 * reliably used for uniquely identifying the driver version.
	 * 
	 * @param driverVersion
	 *            The driver version.
	 */
	public void setDriverVersion(String driverVersion)
	{
		this.driverVersion = driverVersion;
	}

	/**
	 * Get the server-side clusters handled by the driver. This set of clusters
	 * is typically used to match against server-side clusters of a possibly
	 * matching device.
	 * 
	 * @return the Driver matched clusters
	 */
	public HashSet<String> getServerClusters()
	{
		return serverClusters;
	}

	/**
	 * Add one or more (comma separated) server cluster to the set of supported
	 * clusters
	 * 
	 * @param serverClusters
	 */
	public void addServerClusters(String... serverClusters)
	{
		Collections.addAll(this.serverClusters, serverClusters);
	}

	/**
	 * Get the client-side clusters handled by the driver. This set of clusters
	 * is typically used to match against client-side clusters of a possibly
	 * matching device.
	 * 
	 * @return the Driver matched clusters
	 */
	public HashSet<String> getClientClusters()
	{
		return clientClusters;
	}

	/**
	 * Add one or more (comma separated) client cluster to the set of supported
	 * clusters
	 * 
	 * @param clientClusters
	 */
	public void addClientClusters(String... clientClusters)
	{
		Collections.addAll(this.serverClusters, clientClusters);
	}

	/**
	 * Gets the "most general" or default device class for this driver
	 * 
	 * @return
	 */
	public String getMainDeviceClass()
	{
		return mainDeviceClass;
	}

	/**
	 * Sets the "most general" or default device class for this driver
	 * 
	 * @param driverMainClass
	 */
	public void setMainDeviceClass(String driverMainClass)
	{
		this.mainDeviceClass = driverMainClass;
	}
	
	

	public int getBasicClustersCardinality()
	{
		return basicClustersCardinality;
	}

	@Override
	public String toString()
	{
		return "ZigBeeDriverInfo [driverName=" + driverName
				+ ",\n driverVersion=" + driverVersion + ",\n driverMainClass="
				+ mainDeviceClass + ",\n serverClusters=" + serverClusters
				+ ",\n clientClusters=" + clientClusters + "]";
	}

	public int getIntersectionCardinality(Set<String> clientClusters,
			Set<String> serverClusters)
	{
		// compute cluster intersection (Simplest implementation)
		// TODO: improve intersection ranking
		return Sets.getSingleSideIntersection(clientClusters,
				this.clientClusters).size()
				+ Sets.getSingleSideIntersection(serverClusters,
						this.serverClusters).size();
	}
}
