package ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattResponse;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.gattparser.spec.Value;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredAdapter;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.GattCharacteristic;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import storage.DbOperations;
import utils.CallbackListener;
import utils.OSInfo;
import utils.Print;
import utils.ScaleUtils;


public class BleManager {

	private final Logger _logger;
	private OSInfo _osInfo;
	private static DbOperations _db;
	private BluetoothManagerBuilder _bleManagerBuilder;
	public static BluetoothManager _bleManager;
	private ArrayList<CallbackListener> _callbackListeners;
	private BluetoothGattParser _gattParser;
	private ArrayList<DiscoveredDevice> _discoveredDevices;
	private Map<DiscoveredDevice, ArrayList<GattService>> _gattServicesByDevice;
	//private Map<GattService, ArrayList<GattCharacteristic>> _gattCharacteristicByService;
	private Map<URL, Characteristic> _gattCharacteristicByUrl;
	private Map<Characteristic, ArrayList<Field>> _gattFieldsByCharacteristic;


	CharacteristicGovernor characteristicGovernor;

	private final  int ID_START_NIBBLE_CMD = 7;
	private byte startByte; //= (byte) (0xf0 | ID_START_NIBBLE_CMD);
	//private byte startByte2 = (byte) (0xf0 | ID_START_NIBBLE_CMD);
	private final  int ID_START_NIBBLE_INIT = 6;
	private final  int ID_START_NIBBLE_SET_TIME = 9;
	private final  byte CMD_SCALE_STATUS = (byte)0x4f;
	private final byte CMD_SET_UNIT = (byte)0x4d;
	private long unixTime = System.currentTimeMillis() / 1000L;

	public BleManager(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {
		_logger = Logger.getLogger(BleManager.class);
		_osInfo = new OSInfo();
		_db = new DbOperations();
		_callbackListeners = new ArrayList<CallbackListener>();
		_gattParser = BluetoothGattParserFactory.getDefault();

		/*File externalXmlFiles = new File("/ble-manager/ext/gatt");
		_gattParser.loadExtensionsFromFolder(externalXmlFiles.getAbsolutePath());*/

		_discoveredDevices = new ArrayList<DiscoveredDevice>();
		_gattServicesByDevice = new HashMap<DiscoveredDevice, ArrayList<GattService>>();
		//_gattCharacteristicByService = new HashMap<GattService, ArrayList<GattCharacteristic>>();
		_gattCharacteristicByUrl = new HashMap<URL, Characteristic>();
		_gattFieldsByCharacteristic = new HashMap<Characteristic, ArrayList<Field>>();

		buildBleManager(start, activateDiscovering, discoveryRate, refreshRate, rediscover);

		//Search for the SCALE
		_bleManager.addDeviceDiscoveryListener(discoveredDevice -> {

			_logger.info("Discovered device:  " + discoveredDevice.getDisplayName());
			if(discoveredDevice.getDisplayName().contains("BF700")) {

				if (discoveredDevice.getDisplayName().equals("BF700")){
					startByte = (byte) (0xe0 | ID_START_NIBBLE_CMD);
				}
				if (discoveredDevice.getDisplayName().equals("Beurer BF700")){
					startByte = (byte) (0xf0 | ID_START_NIBBLE_CMD);
				}


				deviceUrl = discoveredDevice.getURL().toString().replace("tinyb:/XX:XX:XX:XX:XX:XX/","");
				macaddr=deviceUrl;
				dataUrl = deviceUrl + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;
				device = new Device("SC01", Device.WEIGHT, dataUrl, Device.WEIGHT, false);
				_db.insertDeviceDocument(device);

			}
		});

	}

private void addExternalXMLFiles() {

	}

	/**
	 *
	 * @param start if true, BLE manager will start
	 * @param activateDiscovering if true, the discovery process will be enabled
	 * @param discoveryRate
	 * @param refreshRate
	 * @param rediscover
	 */
	public void buildBleManager(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {
		setBleManagerConfiguration(start, activateDiscovering, discoveryRate, refreshRate, rediscover);
		_bleManager = _bleManagerBuilder.build();
	}

	public void startDiscovery() {
		if(_bleManager != null) {
			if(!_bleManager.isStarted()) {
				_bleManager.start(true);
			}
		}
	}

	public static BluetoothManager getBleManager() {
		return _bleManager;
	}

	private void setBleManagerConfiguration(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {
		_bleManagerBuilder = new BluetoothManagerBuilder();

		_bleManagerBuilder.withStarted(start);
		_bleManagerBuilder.withDiscovering(activateDiscovering);
		_bleManagerBuilder.withIgnoreTransportInitErrors(true);
		setBleManagerTransportType();
		setBleManagerDiscoveryConfiguration(discoveryRate, refreshRate, rediscover);
	}

	/**
	 * If <code>discoveryRate</code> or <code>refreshRate</code> is equal to <code>-1</code>, the
	 * previous value will be unchanged (if the value was never changed, those variables will have
	 * the default value).
	 *
	 * @param discoveryRate in seconds
	 * @param refreshRate in seconds
	 * @param rediscover
	 */
	private void setBleManagerDiscoveryConfiguration(int discoveryRate, int refreshRate, boolean rediscover) {
		if(discoveryRate != -1) {
			_bleManagerBuilder.withDiscoveryRate(discoveryRate);
		}

		if(refreshRate != -1) {
			_bleManagerBuilder.withRefreshRate(refreshRate);
		}

		_bleManagerBuilder.withRediscover(rediscover);

		//_logger.info("Discovery configuration was set");
	}

	private void setBleManagerTransportType() {
		int osType = getOsType();

		switch(osType) {
			case OSInfo.LINUX: {
				_bleManagerBuilder.withTinyBTransport(true);
				//_bleManagerBuilder.withBlueGigaTransport("(/dev/ttyACM)[0-9]{1,3}");
				break;
			}
			case OSInfo.WINDOWS: {
				_bleManagerBuilder.withBlueGigaTransport("(COM)[0-9]{1,3}");
				break;
			}
			case OSInfo.MACOS: {
				_bleManagerBuilder.withBlueGigaTransport("/dev/tty.(usbmodem).*");
				break;
			}
			default: {
				Print.printError("Unknown OS: TinyB and BlueGiga Transport not set");
				break;
			}
		}

		//_logger.info("Operative System: " + osType);
	}

	private int getOsType() {
		return _osInfo.getOsType();
	}

	/* ------------------------------- CALLBACK METHODS ------------------------------- */

	public void addCallbackListener(CallbackListener callbackListener) {
		_callbackListeners.add(callbackListener);
	}

	public void setDeviceListenerScheduler(DiscoveredDevice discoveredDevice) {
		for(CallbackListener callbackListener : _callbackListeners) {
			callbackListener.setDeviceListenerScheduler(discoveredDevice);
		}
	}

	public void notifyDevicesUpdate(DiscoveredDevice discoveredDevice) {
		for(CallbackListener callbackListener : _callbackListeners) {
			callbackListener.updateDiscoveredDevices(discoveredDevice);
		}
	}

	public void notifyResolvedServicesUpdate(URL deviceUrl, ArrayList<GattService> gattServices) {
		for(CallbackListener callbackListener : _callbackListeners) {
			for(GattService gattService : gattServices) {
				callbackListener.updateResolvedServices(deviceUrl.getDeviceAddress(), gattService);
			}
		}
	}

	public void notifyCharacteristicsUpdate(GattService gattService) {
		for(CallbackListener callbackListener : _callbackListeners) {
			for(URL characteristicUrl : _gattCharacteristicByUrl.keySet()) {
				callbackListener.updateCharacteristics(gattService, characteristicUrl);
			}
		}
	}

	public void notifyRawDataReady(byte[] rawData, String fieldName, String characteristicUrl) {
		for(CallbackListener callbackListener : _callbackListeners) {
			callbackListener.updateRawData(rawData, fieldName, characteristicUrl);
		}
	}

	public void notifyParsedDataReady(FieldHolder fieldHolder, String fieldName, String characteristicUrl) {
		String parsedData = fieldHolder.toString();
		String fieldUrl = characteristicUrl + "/" + fieldName;

		for(CallbackListener callbackListener : _callbackListeners) {
			callbackListener.updateParsedData(parsedData, fieldUrl);
		}
	}

	/* -------------------------------- DEVICE METHODS -------------------------------- */

	public void discoverDevices() {
		_bleManager.addDeviceDiscoveryListener(discoveredDevice -> {
			if(discoveredDevice.getDisplayName().equals("Beurer BF700")) {
				/*discoveredDevice.getURL().getDeviceAddress().equals("A0:9E:1A:2C:7C:A4")) {*/
				if(!hasDevice(discoveredDevice)) {
					if(_discoveredDevices.add(discoveredDevice)) {
						notifyDevicesUpdate(discoveredDevice);
						setDeviceListenerScheduler(discoveredDevice);
						_logger.info("New: [" + discoveredDevice.getName() + ", " + discoveredDevice.getURL() + "]");
					}
				}
			}
		});
	}

	private boolean hasDevice(DiscoveredDevice discoveredDevice) {
		return _discoveredDevices.contains(discoveredDevice);
	}

	public ArrayList<DiscoveredDevice> getDiscoveredDevices() {
		return _discoveredDevices;
	}

	public void removeInactiveDevice(DiscoveredDevice deviceToRemove) {
		if(deviceToRemove != null) {
			if(_discoveredDevices.remove(deviceToRemove)) {
				_logger.info("Removed: [" + deviceToRemove.getName() + ", " + deviceToRemove.getURL() + "]");
			}
		}
	}

	public void listDiscoveredDevices(ArrayList<DiscoveredDevice> discoveredDevices) {
		Print.printAllDevicesInfo(discoveredDevices);
	}

	/* ------------------------------- SERVICE METHODS -------------------------------- */

	public void discoverResolvedServicesByDevice(DiscoveredDevice device) {
		URL deviceUrl = device.getURL();
		DeviceGovernor deviceGovernor = _bleManager.getDeviceGovernor(deviceUrl, true);
		ArrayList<GattService> gattServices = new ArrayList<GattService>();

		if(isDeviceGovernorValid(deviceGovernor)) {
			deviceGovernor.whenServicesResolved(DeviceGovernor::getResolvedServices).thenAccept(services -> {
				if(!_gattServicesByDevice.containsKey(device)) {
					_logger.info("New service for [" + device.getName() + ", " + device.getURL() + "]");

					gattServices.addAll(services);
					_gattServicesByDevice.put(device, gattServices);
					//listResolvedServicesByDevice(device);
					notifyResolvedServicesUpdate(deviceUrl, gattServices);
				} else {
					//_logger.warn("Device [" + device.getName() + ", " + device.getURL() + "] already exists");
				}
			});
		}
	}

	public DeviceGovernor getDeviceGovernor(URL deviceUrl) {
		return _bleManager.getDeviceGovernor(deviceUrl, true);
	}

	private boolean isDeviceGovernorValid(DeviceGovernor deviceGovernor) {
		if(deviceGovernor != null) {
			return deviceGovernor.isBleEnabled();
		} else {
			_logger.error("DeviceGovernor is NULL");
		}

		return false;
	}

	public Map<DiscoveredDevice, ArrayList<GattService>> getResolvedServices() {
		return _gattServicesByDevice;
	}

	public void listResolvedServicesByDevice(DiscoveredDevice device) {
		ArrayList<GattService> gattServices = _gattServicesByDevice.get(device);

		_logger.info("List of Services for device [" + device.getURL() + "]:");
		Print.printAllServicesInfo(gattServices);
	}

	public boolean isKnownService(String serviceUuid) {
		return _gattParser.isKnownService(serviceUuid);
	}

	/* --------------------------- CHARACTERISTIC METHODS ---------------------------- */

	public void discoverCharacteristicsByServiceByDevice(GattService gattService) {
		ArrayList<GattCharacteristic> gattCharacteristics = new ArrayList<GattCharacteristic>();
		gattCharacteristics.addAll(gattService.getCharacteristics());

		buildCharacteristicByUrlMap(gattCharacteristics);
		notifyCharacteristicsUpdate(gattService);
	}

	public Characteristic getCharacteristicByUrl(URL characteristicUrl) {
		if(characteristicUrl != null) {
			for(URL url : _gattCharacteristicByUrl.keySet()) {
				if(url.equals(characteristicUrl)) {
					return _gattCharacteristicByUrl.get(url);
				}
			}
		}

		return null;
	}

	/**
	 * REVIEW METHOD NAME
	 *
	 * @param gattCharacteristics
	 */
	private void buildCharacteristicByUrlMap(ArrayList<GattCharacteristic> gattCharacteristics) {
		Characteristic characteristic;
		URL characteristicUrl;

		for(GattCharacteristic gattCharacteristic : gattCharacteristics) {
			characteristicUrl = gattCharacteristic.getURL();
			if(!_gattCharacteristicByUrl.containsKey(characteristicUrl)) {
				characteristic = castGattCharacteristicToCharacteristic(gattCharacteristic);
				_gattCharacteristicByUrl.put(characteristicUrl, characteristic);
			} else {
				  //_logger.warn("CharacteristicByUrl Map already contains " + characteristicUrl);
			}
		}
	}

	private Characteristic castGattCharacteristicToCharacteristic(GattCharacteristic gattCharacteristic) {
		URL characteristicUrl = gattCharacteristic.getURL();
		String characteristicUuid = characteristicUrl.getCharacteristicUUID();
		Characteristic characteristic = _gattParser.getCharacteristic(characteristicUuid);

		return characteristic;
	}

	public void listCharacteristicsByServiceByDevice(Map<DiscoveredDevice, ArrayList<GattService>> gattServicesByDevice) {
		for(DiscoveredDevice device : gattServicesByDevice.keySet()) {
			for(GattService service : gattServicesByDevice.get(device)) {
				listCharacteristicsByService(service);
			}
		}
	}

	private void listCharacteristicsByService(GattService gattService) {
		ArrayList<GattCharacteristic> gattCharacteristics = new ArrayList<GattCharacteristic>();
		gattCharacteristics.addAll(gattService.getCharacteristics());

		Print.printAllCharacteristicsInfo(gattCharacteristics);
	}

/*	public void parseData(URL characteristicUrl) {
		CharacteristicGovernor characteristicGovernor = _bleManager.getCharacteristicGovernor(characteristicUrl);

		characteristicGovernor.whenReady(CharacteristicGovernor::isReadable).thenAccept(readable -> {
			if(readable) {
				_logger.warn("The Characteristic with URL " + characteristicUrl + " is readable");

				characteristicGovernor.whenReady(CharacteristicGovernor::read).thenAccept(rawData -> {
					Characteristic characteristic = _gattCharacteristicByUrl.get(characteristicUrl);

					if(characteristic != null) {
						if(!_gattFieldsByCharacteristic.containsKey(characteristic)) {
							buildFieldsByCharacteristicMap(characteristic);

							FieldHolder fieldHolder;
							String fieldName;
							String serviceUrl = characteristicUrl.getDeviceAddress() + "/" + characteristicUrl.getServiceUUID();
							String characUrl = serviceUrl + "/" + characteristicUrl.getCharacteristicUUID();

							for(Field field : _gattFieldsByCharacteristic.get(characteristic)) {
								fieldName = field.getName();
								fieldHolder = getParsedData(rawData, fieldName, characteristic);

								notifyRawDataReady(rawData, fieldName, characUrl);
								notifyParsedDataReady(fieldHolder, fieldName, characUrl);
							}
						} else {
							  //_logger.warn("FieldsByCharacteristic Map already contains " + characteristic.getName());
						}
					} else {
						//_logger.warn("There is no characteristic with URL " + characteristicUrl);
					}
			    });
			} else {
				_logger.warn("The Characteristic with URL " + characteristicUrl + " is NOT readable");

				characteristicGovernor.whenReady(CharacteristicGovernor::isNotifiable).thenAccept(notifiable -> {
					if(notifiable) {
						characteristicGovernor.addValueListener(notificationData -> {
							Characteristic characteristic = _gattCharacteristicByUrl.get(characteristicUrl);

							if(characteristic != null) {
								if(!_gattFieldsByCharacteristic.containsKey(characteristic)) {
									buildFieldsByCharacteristicMap(characteristic);
								} else {
									//_logger.warn("FieldsByCharacteristic Map already contains (Notification) " + characteristic.getName());
								}

								FieldHolder fieldHolder;
								String fieldName;
								String serviceUrl = characteristicUrl.getDeviceAddress() + "/" + characteristicUrl.getServiceUUID();
								String characUrl = serviceUrl + "/" + characteristicUrl.getCharacteristicUUID();

								for(Field field : _gattFieldsByCharacteristic.get(characteristic)) {
									fieldName = field.getName();
									fieldHolder = getParsedData(notificationData, fieldName, characteristic);
									_logger.info("Parsed data of characteristic " + characUrl);
									if(fieldHolder != null) {
										notifyRawDataReady(notificationData, fieldName, characUrl);
										notifyParsedDataReady(fieldHolder, fieldName, characUrl);
									}
								}
							} else {
								//_logger.warn("There is no characteristic with URL (Notification) " + characteristicUrl);
							}
						});
					}
				});
			}
		});
	}*/



	private boolean scaleInitialized=false;
	private float weight=-1;
	private boolean scaleReady=false;
	private boolean standby=true;
	private boolean authenticated=false;

	public synchronized void resetWeight(){

		this.weight = -1;

	}

	public synchronized float getWeight(){

		return this.weight;

	}

	private synchronized void setWeight(float weight){

		this.weight=weight;

	}

	private synchronized void setScaleInitialized(){

		scaleInitialized = true;

	}

	public synchronized void setScaleNotInitialized(){

		scaleInitialized = false;

	}

	public synchronized boolean getScaleInitialized(){

		return this.scaleInitialized;

	}

	public synchronized boolean getScaleConnected(){

		return this.scaleConnected;

	}

	public synchronized void setScaleNotConnected(){

		this.scaleConnected = false;
	}

	public synchronized void setScaleStandbyTrue(){

		this.standby = true;
	}

	public synchronized void setScaleStandbyFalse(){

		this.standby = false;
	}

	public synchronized String getScaleAddr(){

		return macaddr;
	}

	public synchronized boolean getScaleAuthenticated(){

		_logger.info("Get scale  authenticated: " + this.authenticated);
		return this.authenticated;
	}


	public synchronized void setScaleAuthenticated(){

		this.authenticated = true;
		_logger.info("Set scale authenticated: " + this.authenticated);
	}

	public synchronized void setScaleNotAuthenticated(){

		this.authenticated = false;
		_logger.info("Set scale not authenticated: " + this.authenticated);
	}

	private synchronized boolean getScaleStandby(){

		return standby;
	}

	boolean personOnTop=false;

	public synchronized boolean getPersonOnTop(){

		return this.personOnTop;

	}

	public synchronized void setPersonOnTop(boolean status){

		personOnTop=status;

	}


	String serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
	String characteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb";
	String deviceUrl;
	String macaddr;
	String dataUrl;
	Boolean scaleConnected = false;
	String fieldName = "Weight Measurement Value (Kg)";
	Device device;


	public void listenNewData() {


		_logger.info("Listening new data");
		ArrayList<Device> devices = _db.getDevices();
		//System.out.println("Discovered devices: "+ _bleManager.getDiscoveredDevices());

		if (!getScaleStandby()){
			for(Device device : devices) {
				//if(device.getDeviceType()!="Weight") {
				String dataUrl = device.getDataUrl();
				String adapterUrl = getAdapterUrl();

				if (!this.scaleConnected){
					enableDeviceConnection(adapterUrl, dataUrl);

				}

				String deviceAddress = dataUrl.substring(0, dataUrl.indexOf("/"));
				URL deviceUrl = new URL(adapterUrl + "/" + deviceAddress);
				_bleManager.getDeviceGovernor(deviceUrl).whenReady(DeviceGovernor::isConnected).thenAccept(connected -> {
					_logger.info("Characteristic is connected:  " + connected);
						});
				/*if (!_bleManager.getDeviceGovernor(deviceUrl).isConnected()){
					this.scaleConnected=false;
					this.scaleInitialized=false;
				}*/


				if (getScaleAuthenticated()){
					_logger.info("Is authenticated");
					URL characteristicUrl = new URL(adapterUrl + "/" + dataUrl.substring(0, dataUrl.lastIndexOf("/")));
					characteristicGovernor = _bleManager.getCharacteristicGovernor(characteristicUrl);

					characteristicGovernor.whenReady(CharacteristicGovernor::isNotifiable).thenAccept(notifiable -> {
						_logger.info("Characteristic is notifiable:  " + characteristicGovernor.isNotifiable());
						_logger.info("Characteristic is notifying:  " + characteristicGovernor.isNotifying());

						if (!characteristicGovernor.isNotifying()){
							characteristicGovernor.addValueListener(notificationData -> {

								//if first byte = startByte notification belongs to scale
								///Uncomment to see Raw Notification
								System.out.print("\nNotification raw = {");
								for (byte b : notificationData) {
									System.out.print(String.format("%02x,", b));
									System.out.print("}");
								}

								if (notificationData[0] == startByte) {

									if (notificationData[2]==CMD_SCALE_STATUS){

										final int batteryLevel = notificationData[4] & 0xFF;
										_logger.info("Battery:  " + batteryLevel);

									}

									if (notificationData[1]==88) {
										setPersonOnTop(true);
										boolean stableMeasurement = notificationData[2] == 0;
										float weight = ScaleUtils.getKiloGram(notificationData, 3);

										if (stableMeasurement) {
											_logger.info("Weight:  " + weight);
											setWeight(weight);
											setPersonOnTop(false);
										}

									}
								}else{
									FieldHolder fieldHolder = getParsedData(notificationData, dataUrl, characteristicUrl);
									if(fieldHolder != null) {
										String parsedData = fieldHolder.toString();
										_db.insertDataDocument(device, parsedData);

									}

								}

							});
						}

						if(notifiable) {
							_logger.info("Standby status:  " + getScaleStandby());

							if (!getScaleInitialized()){
								byte[] command = ScaleUtils.sendAlternativeStartCode(ID_START_NIBBLE_INIT, (byte) 0x01);
								characteristicGovernor.write(command);
								command = ScaleUtils.sendAlternativeStartCode(ID_START_NIBBLE_SET_TIME, ScaleUtils.toInt32Be(unixTime));
								characteristicGovernor.write(command);
								command = ScaleUtils.sendCommand(CMD_SCALE_STATUS, ScaleUtils.encodeUserId(null));
								characteristicGovernor.write(command);
								command = ScaleUtils.sendCommand(CMD_SET_UNIT, (byte) 1);
								characteristicGovernor.write(command);
								setScaleInitialized();

							}
						}

					});
				}
			}
		}
		_logger.info("Exiting listening new data");
	}



	private String getAdapterUrl() {
		ArrayList<DiscoveredAdapter> adapters = new ArrayList<DiscoveredAdapter>(_bleManager.getDiscoveredAdapters());
		DiscoveredAdapter adapter = adapters.get(0);
		String protocolAndAdapterUrl = adapter.getURL().toString();
		String adapterUrl = protocolAndAdapterUrl.substring(protocolAndAdapterUrl.indexOf("/"));
		_logger.info("Adapter URL " + adapterUrl);

		return adapterUrl;
	}

	private void enableDeviceConnection(String adapterUrl, String dataUrl) {
		String deviceAddress = dataUrl.substring(0, dataUrl.indexOf("/"));
		URL deviceUrl = new URL(adapterUrl + "/" + deviceAddress);
		_bleManager.getDeviceGovernor(deviceUrl, true);

		_logger.info("Connection enabled to device " + deviceUrl);
		macaddr=deviceAddress;
		this.scaleConnected=true;
	}

	private void buildFieldsByCharacteristicMap(Characteristic characteristic) {
		Value value = characteristic.getValue();
		ArrayList<Field> fields = new ArrayList<Field>();
		fields.addAll(value.getFields());

		_gattFieldsByCharacteristic.put(characteristic, fields);
	}

	private FieldHolder getParsedData(byte[] rawData, String dataUrl, URL characteristicUrl) {
		String fieldName = dataUrl.substring(dataUrl.lastIndexOf("/") + 1);
		String characteristicUuid = characteristicUrl.getCharacteristicUUID();
		GattResponse gattResponse = _gattParser.parse(characteristicUuid, rawData);
		FieldHolder fieldHolder = gattResponse.get(fieldName);
		//_logger.info("Parsed Data: [" + fieldName + ", " + fieldHolder + "]");

		return fieldHolder;
	}


	//
	 // OLD VERSION
	 // @param rawData
	 // @param fieldName
	 // @param characteristic
	 // @return
	 //
	/*private FieldHolder getParsedData(byte[] rawData, String fieldName, Characteristic characteristic) {
		GattResponse gattResponse = _gattParser.parse(characteristic.getUuid(), rawData);
		FieldHolder fieldHolder = gattResponse.get(fieldName);

		_logger.info(" -----> PARSED DATA: [" + fieldName + ", " + fieldHolder + "]");

		return fieldHolder;
	}*/


/*	public void parseData(URL characteristicUrl) {
		CharacteristicGovernor characteristicGovernor = _bleManager.getCharacteristicGovernor(characteristicUrl);

		characteristicGovernor.whenReady(CharacteristicGovernor::isReadable).thenAccept(readable -> {
			if(readable) {
				characteristicGovernor.whenReady(CharacteristicGovernor::read).thenAccept(rawData -> {
					Characteristic characteristic = _gattCharacteristicByUrl.get(characteristicUrl);

					if(characteristic != null) {
						if(!_gattFieldsByCharacteristic.containsKey(characteristic)) {
							Value value = characteristic.getValue();
							ArrayList<Field> fields = new ArrayList<Field>();
							fields.addAll(value.getFields());

							_gattFieldsByCharacteristic.put(characteristic, fields);
							//_logger.warn("FieldCharacteristicMap has new characteristic: " + gattCharacteristic.getURL().getCharacteristicUUID());

							FieldHolder fieldHolder;
							GattResponse gattResponse = _gattParser.parse(characteristic.getUuid(), rawData);

							for(Field field : _gattFieldsByCharacteristic.get(characteristic)) {
								fieldHolder = gattResponse.get(field.getName());
								_logger.info(" -----> PARSED DATA: [" + field.getName() + ", " + fieldHolder + "]");
							}
						} else {
							  _logger.warn("FieldsByCharacteristic Map already contains " + characteristic.getName());
						}
					} else {
						_logger.warn("There is no characteristic with URL " + characteristicUrl);
					}
			    });
			} else {
				_logger.warn("The Characteristic with URL " + characteristicUrl + " is not readable");
			}
		});
	}*/

	public boolean isKnownCharacteristic(String characteristicUuid) {
		return _gattParser.isKnownCharacteristic(characteristicUuid);
	}

	public static DbOperations getDb() {
		return _db;
	}
}
