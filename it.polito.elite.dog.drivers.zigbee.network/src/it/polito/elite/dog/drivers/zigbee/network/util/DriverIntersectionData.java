/**
 * 
 */
package it.polito.elite.dog.drivers.zigbee.network.util;

import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeApplianceInfo;
import it.polito.elite.dog.drivers.zigbee.network.info.ZigBeeDriverInfo;

/**
 * @author bonino
 * 
 */
public class DriverIntersectionData implements
		Comparable<DriverIntersectionData>
{

	private ZigBeeDriverInfo driverInfo;
	private int cardinality;
	private ZigBeeApplianceInfo applianceInfo;
	private double driverCompleteness;
	private double applianceCompleteness;
	private double score;
	private boolean isPerfect;
	private static double MAX_SCORE=1.0;
	private static double MAX_RAW_SCORE=5.0; //MIN_RAW_SCORE = 0.0

	/**
	 * 
	 */
	public DriverIntersectionData(ZigBeeDriverInfo driverInfo,
			ZigBeeApplianceInfo applianceInfo)
	{
		// the cardinality of the intersection between driver and appliance
		this.cardinality = driverInfo.getIntersectionCardinality(
				applianceInfo.getClientClusters(),
				applianceInfo.getServerClusters());

		// the driver details
		this.driverInfo = driverInfo;

		// the appliance details
		this.applianceInfo = applianceInfo;

		// compute the driverSideCompleteness
		this.driverCompleteness = (double) this.cardinality
				/ (double)(this.driverInfo.getClientClusters().size() + this.driverInfo
						.getServerClusters().size());

		// compute the appliance side completeness
		this.applianceCompleteness = (double)this.cardinality
				/ (double)(this.applianceInfo.getClientClusters().size() + this.applianceInfo
						.getServerClusters().size());

		if((this.applianceCompleteness+this.driverCompleteness) == 2)
		{
			//perfect match
			this.isPerfect = true;
			
			//set the score at maximum
			this.score = DriverIntersectionData.MAX_SCORE;
		}
		else
		{
			// not perfect
			this.isPerfect = false;
			
			// compute the intersection score (between 0 and 1)
			this.score = ((2 * Math.floor(this.driverCompleteness) + Math
					.floor(this.applianceCompleteness))
					+ this.applianceCompleteness
					+ (3 - Math.ceil(Math.abs((this.applianceCompleteness - 1))) - Math
							.ceil(Math.abs((this.driverCompleteness - 1))))
					* this.driverCompleteness)/DriverIntersectionData.MAX_RAW_SCORE;
		}
		

	}

	public ZigBeeDriverInfo getDriverInfo()
	{
		return driverInfo;
	}
	

	public ZigBeeApplianceInfo getApplianceInfo()
	{
		return applianceInfo;
	}

	public int getCardinality()
	{
		return cardinality;
	}

	public double getDriverCompleteness()
	{
		return driverCompleteness;
	}

	public double getApplianceCompleteness()
	{
		return applianceCompleteness;
	}

	public double getScore()
	{
		return score;
	}

	public boolean isPerfect()
	{
		return isPerfect;
	}

	@Override
	public int compareTo(DriverIntersectionData o)
	{
		return Double.compare(this.score, o.score);
	}

	@Override
	public String toString()
	{
		return "DriverIntersectionData [cardinality=" + cardinality
				+ ", driverCompleteness=" + driverCompleteness
				+ ", applianceCompleteness=" + applianceCompleteness
				+ ", score=" + score + ", isPerfect=" + isPerfect + "]";
	}
	
	

}
