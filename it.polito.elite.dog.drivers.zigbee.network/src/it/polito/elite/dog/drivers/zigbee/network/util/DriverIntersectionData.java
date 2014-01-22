/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.network.util;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;

/**
 * @author bonino
 *
 */
public class DriverIntersectionData implements Comparable<DriverIntersectionData>
{

	private ZigBeeDriverInfo driverInfo;
	private int cardinality;

	/**
	 * 
	 */
	public DriverIntersectionData(int cardinality, ZigBeeDriverInfo driverInfo)
	{
		this.cardinality = cardinality;
		this.driverInfo = driverInfo;
	}

	public ZigBeeDriverInfo getDriverInfo()
	{
		return driverInfo;
	}

	public void setDriverInfo(ZigBeeDriverInfo driverInfo)
	{
		this.driverInfo = driverInfo;
	}

	public int getCardinality()
	{
		return cardinality;
	}

	public void setCardinality(int cardinality)
	{
		this.cardinality = cardinality;
	}

	@Override
	public int compareTo(DriverIntersectionData o)
	{
		return  Integer.compare(this.cardinality, o.cardinality);
	}

	
}
