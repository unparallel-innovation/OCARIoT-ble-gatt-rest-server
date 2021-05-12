import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ble.BlutoothBeurer;
import org.apache.log4j.Logger;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.GattService;

import ble.Device;
import ble.BleManager;
import rest.RestServer;
import storage.DbOperations;
import utils.CallbackListener;

public class Main /*implements CallbackListener*/ {
    
	final static Logger _logger = Logger.getLogger(Main.class);
	
    private static BleManager _bleManager;
    private static DbOperations _db;
	private static BlutoothBeurer bluetoothBeurer;

    
    public Main() {
    	_bleManager = new BleManager(true, true, -1, -1, false);
    	//_bleManager.addCallbackListener(this);
		//bluetoothBeurer = new BlutoothBeurer(true, true, -1, -1, false);
    	//_db = BleManager.getDb();
    	new RestServer(-1, _bleManager);

		//Search for the SCALE
		/*_bleManager._bleManager.addDeviceDiscoveryListener(discoveredDevice -> {

			String serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
			String characteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb";
			String deviceUrl;
			String dataUrl;
			String fieldName = "Weight Measurement Value (Kg)";
			Boolean found = false;
			Device device;

			if(discoveredDevice.getDisplayName().equals("Beurer BF700")) {

				deviceUrl = discoveredDevice.getURL().toString().replace("tinyb:/XX:XX:XX:XX:XX:XX/","");
				dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
				device = new Device("SC01", Device.WEIGHT, dataUrl, Device.WEIGHT, false);
				_db.insertDeviceDocument(device);
				found=true;

			}
		});*/
    }
    
	public static void main(String[] args) throws IllegalArgumentException, InterruptedException {
		new Main();
		
		/*if(args.length > 0) {
			if(args[0].equals("export")) {
				_db.exportEachCollectionIndividuallyToJson();
			}
		}*/

		_logger.info("Starting...");


		
		/*String deviceUrl = "A0:9E:1A:2C:7C:A4";
		String serviceUuid = "0000180d-0000-1000-8000-00805f9b34fb";
		String characteristicUuid = "00002a37-0000-1000-8000-00805f9b34fb";
		String fieldName = "Heart Rate Measurement Value (uint8)";
		String dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
		Device device = new Device("HR01", Device.BAND, dataUrl, Device.HEART_RATE, false);
		_db.insertDeviceDocument(device);
		
		deviceUrl = "E4:14:82:58:E9:AF";
		serviceUuid = "0000181a-0000-1000-8000-00805f9b34fb";
		characteristicUuid = "00002a6f-0000-1000-8000-00805f9b34fb";
		fieldName = "Humidity";
		dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
		device = new Device("HUM01", Device.SENSOR, dataUrl, Device.HUMIDITY, false);
		//_db.insertDeviceDocument(device);
		
		deviceUrl = "E4:14:82:58:E9:AF";
		serviceUuid = "00001809-0000-1000-8000-00805f9b34fb";
		characteristicUuid = "00002a1c-0000-1000-8000-00805f9b34fb";
		fieldName = "Temperature Measurement Value (Celsius)";
		dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
		device = new Device("TMP01", Device.SENSOR, dataUrl, Device.TEMPERATURE, false);
		//_db.insertDeviceDocument(device);

		deviceUrl = "00:13:04:1A:9F:5F";
		serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
		characteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb";
		fieldName = "Weight Measurement Value (Kg)";
		dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
		device = new Device("SCL01", Device.SENSOR, dataUrl, Device.WEIGHT, false);
		_db.insertDeviceDocument(device);*/

		/*BlutoothBeurer bluetoothBeurer = new BlutoothBeurer();
		bluetoothBeurer.start();*/
		//bluetoothBeurer.jumpNextToStepNr(0);
		//bluetoothBeurer.resumeMachineState();
        //Thread.currentThread().join();

		//setDataListenerScheduler();

		//generateHRDummyData();
		//generateHumDummyData();
		
		/*String serviceUuid = "0000fa18-0000-1000-8000-00805f9b34fb";
		String characteristicUuid = "0000fa32-0000-1000-8000-00805f9b34fb";
		String fieldName = "SPL SLOW";
		Band band = new Band("HR02", dataUrl, false);*/

		
		//_bleManager.listenNewData();
		setDataListenerScheduler();
		//_bleManager.startDiscovery();
		_bleManager.discoverDevices();
	}

	public static void setDataListenerScheduler() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> _bleManager.listenNewData(), 3, 6, TimeUnit.SECONDS);
	}

	/*public void updateDiscoveredDevices(DiscoveredDevice discoveredDevice) {
		_db.insertDeviceDocument(discoveredDevice);
		_bleManager.discoverResolvedServicesByDevice(discoveredDevice);
	}*/
	
	/*@Override
	public void updateResolvedServices(String deviceUrl, GattService gattService) {
		if(_db.insertServiceDocument(gattService)) {
			_db.addServicesToDevice(deviceUrl);
		}
		
		//String serviceUuid = gattService.getURL().getServiceUUID();
		//if(serviceUuid.equals("0000180d-0000-1000-8000-00805f9b34fb")) {
			_bleManager.discoverCharacteristicsByServiceByDevice(gattService);
		//}
	}*/

	/*@Override
	public void updateCharacteristics(GattService gattService, URL characteristicUrl) {
		String serviceUrl = gattService.getURL().getDeviceAddress() + "/" + gattService.getURL().getServiceUUID();
		String characteristicUuid = characteristicUrl.getCharacteristicUUID();
		String characUrl = serviceUrl + "/" + characteristicUuid;
		
		if(_bleManager.isKnownCharacteristic(characteristicUuid)) {
			//_logger.info("Main was notified of new characteristic: " + characteristicUrl);
			Characteristic characteristic = _bleManager.getCharacteristicByUrl(characteristicUrl);
			
			if(_db.insertCharacteristicDocument(characUrl, characteristic)) {
				_db.addCharacteristicsToService(serviceUrl);
			}
			
			//_bleManager.parseData(characteristicUrl);
		} else {
			//_logger.warn("Unknown characteristic: " + characUrl);
			addUnknownCharacteristicToFile(characUrl);
		}
	}
	
	@Override
	public void updateRawData(byte[] rawData, String fieldName, String characteristicUrl) {
		if(_db.insertFieldDocument(rawData, fieldName, characteristicUrl)) {
			_db.addFieldsToCharacteristic(characteristicUrl);
		}
	}
	
	@Override
	public void updateParsedData(String parsedData, String fieldUrl) {
		//_db.insertDataDocument(parsedData, fieldUrl);
		_db.addDataToField(fieldUrl);
	}
	
	private void addUnknownCharacteristicToFile(String characteristicUrl) {
		String filepath = "/usr/project/ble-manager-jar/unknownCharacteristics.txt";
		
		try {
			//_logger.info("Writing unknown characteristic to file...");
			
			FileWriter fileWriter = new FileWriter(filepath, true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			PrintWriter filePrinter = new PrintWriter(bufferedWriter);
			
			filePrinter.println(characteristicUrl);
			
			filePrinter.close();
			bufferedWriter.close();
			fileWriter.close();
		} catch (IOException e) {
			_logger.error("There is an IOException");
			e.printStackTrace();
		}
	}*/

	/*public void setDeviceListenerScheduler(DiscoveredDevice discoveredDevice) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			DeviceGovernor deviceGovernor = _bleManager.getDeviceGovernor(discoveredDevice.getURL());
			if(!deviceGovernor.isOnline() && !deviceGovernor.isConnected()) {
				_bleManager.removeInactiveDevice(discoveredDevice);
				scheduler.shutdown();
				
				try {
					if(!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
						scheduler.shutdownNow();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 30, 60, TimeUnit.SECONDS);
	}
	


	public static void generateHRDummyData() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			String deviceUrl = "A0:9E:1A:2C:7C:A4";
			String serviceUuid = "0000180d-0000-1000-8000-00805f9b34fb";
			String characteristicUuid = "00002a37-0000-1000-8000-00805f9b34fb";
			String fieldName = "Heart Rate Measurement Value (uint8)";
			String dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
			Device device = new Device("HR01", Device.BAND, dataUrl, Device.HEART_RATE, false);
			_db.insertDataDocument(device, "100");
		}, 5, 1, TimeUnit.SECONDS);
	}

	public static void generateHumDummyData() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			String deviceUrl = "E4:14:82:58:E9:AF";
			String serviceUuid = "0000181a-0000-1000-8000-00805f9b34fb";
			String characteristicUuid = "00002a6f-0000-1000-8000-00805f9b34fb";
			String fieldName = "Humidity";
			String dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
			Device device = new Device("HUM01", Device.SENSOR, dataUrl, Device.HUMIDITY, false);
			_db.insertDataDocument(device, "34.3");
			
		}, 5, 10, TimeUnit.SECONDS);
	}*/
}
