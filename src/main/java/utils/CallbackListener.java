package utils;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.GattService;

public interface CallbackListener {

	void updateDiscoveredDevices(DiscoveredDevice discoveredDevice);
	
	void updateResolvedServices(String deviceUrl, GattService gattService);
	
	void updateCharacteristics(GattService gattService, URL characteristicUrl);
	
	void updateRawData(byte[] rawData, String fieldName, String characteristicUrl);
	
	void updateParsedData(String parsedData, String fieldUrl);
	
	void setDeviceListenerScheduler(DiscoveredDevice discoveredDevice);
}
