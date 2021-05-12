package utils;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.GattCharacteristic;
import org.sputnikdev.bluetooth.manager.GattService;

public class Print {

	final static Logger _logger = Logger.getLogger(Print.class);
	
	private Print() {}
	
	public static void printWarning(String warningMessage) {
		_logger.warn(warningMessage);
	}
	
	public static void printError(String errorMessage) {
		_logger.error(errorMessage);
	}
	
	private static void printDeviceInfo(DiscoveredDevice device) {
		_logger.info("Device: " + device.getName() + " | " + "URL: " + device.getURL());
	}
	
	private static void printServiceInfo(GattService gattService) {
		_logger.info("Service: " + gattService.getURL());
	}
	
	private static void printCharacteristicInfo(GattCharacteristic gattCharacteristic) {
		_logger.info("Characteristic: " + gattCharacteristic.getURL());
	}
	
	public static void printAllDevicesInfo(ArrayList<DiscoveredDevice> discoveredDevices) {
		_logger.info("List of Devices:");
		for(DiscoveredDevice device : discoveredDevices) {
			printDeviceInfo(device);
		}
	}
	
	public static void printAllServicesInfo(ArrayList<GattService> gattServices) {
		for(GattService gattService : gattServices) {
			printServiceInfo(gattService);
		}
	}
	
	public static void printAllCharacteristicsInfo(ArrayList<GattCharacteristic> gattCharacteristics) {
		for(GattCharacteristic gattCharacteristic : gattCharacteristics) {
			printCharacteristicInfo(gattCharacteristic);
		}
	}
}
