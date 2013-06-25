/*
 * Dog 2.0 - ZigBee Network Driver
 * 
 * Copyright [Jun 25, 2013] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zigbee.network.info;

/**
 * @author bonino
 * 
 */
public class ZigBeeInfo
{
	/**
	 * the manufacturer identifier (Modbus)
	 */
	public static final String MANUFACTURER = "ZigBee";
	
	/**
	 * the command name to which a specific configuration refers
	 */
	public static final String COMMAND_NAME = "realCommandName";
	
	/**
	 * the notification name to which a specific configuration refers
	 */
	public static final String NOTIFICATION_NAME = "notificationName";
	
	/**
	 * The appliance serial number
	 */
	public static final String ZIGBEE_APP_SERIAL = "serialNumber";
	
	/**
	 * The unit of measure identifier
	 */
	public static final String ZIGBEE_UOM = "unitOfMeasure";
	
	/**
	 * The scale factor identifier
	 */
	public static final String ZIGBEE_SCALE_FACTOR = "scaleFactor";
}
