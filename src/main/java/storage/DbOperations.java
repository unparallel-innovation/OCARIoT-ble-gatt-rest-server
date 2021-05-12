package storage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dizitart.no2.Cursor;
import org.dizitart.no2.Document;
import org.dizitart.no2.Filter;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.PersistentCollection;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.tool.ExportOptions;
import org.dizitart.no2.tool.Exporter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.GattService;

import ble.Device;

public class DbOperations {
	
	/* ------------------------------ FILEPATH CONSTANTS ------------------------------ */
	private static final String FILEPATH = System.getProperty("user.home") + "/ble_gatt-rest-api-server/";
	private static final String DB_FILEPATH = FILEPATH + "data.db";
	private static final String DB_JSON = FILEPATH + "db.json";
	private static final String DEVICES_JSON = FILEPATH + "devices.json";
	/*private static final String DEVICES_JSON = FILEPATH + "devices.json";
	private static final String SERVICES_JSON = FILEPATH + "services.json";
	private static final String CHARACTERISTICS_JSON = FILEPATH + "characteristics.json";
	private static final String FIELDS_JSON = FILEPATH + "fields.json";*/
	private static final String DATA_JSON = FILEPATH + "data.json";
	private static final String ERROR_JSON = FILEPATH + "error.json"; // This should never happen
	
	/* ----------------------------- COLLECTION CONSTANTS ----------------------------- */
	private static final String DEVICES_COLLECTION = "devices";
	/*private static final String DEVICES_COLLECTION = "devices";*/
	//private static final String SERVICES_COLLECTION = "services";
	/*private static final String CHARACTERISTICS_COLLECTION = "characteristics";
	private static final String FIELDS_COLLECTION = "fields";*/
	private static final String DATA_COLLECTION = "data";
	
	/* ---------------------------------- VARIABLES ----------------------------------- */
	private final Logger _logger;
	private Nitrite _db;
	
	/* --------------------------------- CONSTRUCTOR ---------------------------------- */
	public DbOperations() {
		_logger = Logger.getLogger(DbOperations.class);

		System.out.println("Creating database");
		_db = Nitrite.builder().compressed().filePath(DB_FILEPATH).openOrCreate();
		if(_db==null){

			System.out.println("Could not open or create Database");

		}
		createAllCollections();
		setIndexesToAllCollections();
	}
	
	/* --------------------------- SETUP COLLECTION METHODS --------------------------- */
	private void createAllCollections() {
		_db.getCollection(DEVICES_COLLECTION);
		/*_db.getCollection(DEVICES_COLLECTION);
		_db.getCollection(SERVICES_COLLECTION);
		_db.getCollection(CHARACTERISTICS_COLLECTION);
		_db.getCollection(FIELDS_COLLECTION);*/
		_db.getCollection(DATA_COLLECTION);
	}
	
	private void setIndexesToAllCollections() {
		setIndexToCollection("deviceId", DEVICES_COLLECTION, IndexType.Unique);
		/*setIndexToCollection("deviceUrl", DEVICES_COLLECTION, IndexType.Unique);
		setIndexToCollection("serviceUrl", SERVICES_COLLECTION, IndexType.Unique);
		setIndexToCollection("characteristicUrl", CHARACTERISTICS_COLLECTION, IndexType.Unique);
		setIndexToCollection("fieldUrl", FIELDS_COLLECTION, IndexType.Unique);*/
		setIndexToCollection("dataUrl", DATA_COLLECTION, IndexType.NonUnique);
	}
	
	private void setIndexToCollection(String fieldToIndex, String collectionName, IndexType indexType) {
		if((fieldToIndex != null) && (collectionName != null)) {
			NitriteCollection collection = _db.getCollection(collectionName);
			
			if(!collection.hasIndex(fieldToIndex)) {
				collection.createIndex(fieldToIndex, IndexOptions.indexOptions(indexType));
			}
		}
	}
	
	/* ----------------------------- EXPORT DATA METHODS ------------------------------ */
	public void exportDbToJson() {
		Exporter dbExporter = Exporter.of(_db);
		dbExporter.exportTo(DB_JSON);
		
		_logger.info("DB was exported successfully");
	}
	
	public void exportEachCollectionIndividuallyToJson() {
		exportCollectionToJson(DbOperations.DEVICES_COLLECTION);
		/*exportCollectionToJson(DbOperations.DEVICES_COLLECTION);
    	exportCollectionToJson(DbOperations.SERVICES_COLLECTION);
    	exportCollectionToJson(DbOperations.CHARACTERISTICS_COLLECTION);
    	exportCollectionToJson(DbOperations.FIELDS_COLLECTION);*/
    	exportCollectionToJson(DbOperations.DATA_COLLECTION);
	}
	
	private void exportCollectionToJson(String collectionName) {
		PersistentCollection<?> collection = _db.getCollection(collectionName);
		List<PersistentCollection<?>> collections = new ArrayList<PersistentCollection<?>>();
		collections.add(collection);
		
		String jsonFilepath = getFilepathToExportToJson(collectionName);
		ExportOptions collectionExportOptions = new ExportOptions();
		collectionExportOptions.setCollections(collections);
		
		Exporter.of(_db).withOptions(collectionExportOptions).exportTo(jsonFilepath);
		
		_logger.info("Collection " + collectionName + " was exported successfully");
	}
	
	private String getFilepathToExportToJson(String collectionName) {
		switch(collectionName) {
			case DEVICES_COLLECTION: return DEVICES_JSON;
			/*case DEVICES_COLLECTION: return DEVICES_JSON;
			case SERVICES_COLLECTION: return SERVICES_JSON;
			case CHARACTERISTICS_COLLECTION: return CHARACTERISTICS_JSON;
			case FIELDS_COLLECTION: return FIELDS_JSON;*/
			case DATA_COLLECTION: return DATA_JSON;
			default: return ERROR_JSON;
		}
	}
	   
	/* ---------------------------------- GET METHODS --------------------------------- */
	public ArrayList<Device> getDevices() {
		Device device;
		boolean read;
		String deviceId, deviceType, dataUrl, dataType;
		ArrayList<Device> devices = new ArrayList<Device>();
		NitriteCollection devicesCollection = _db.getCollection(DEVICES_COLLECTION);
		Cursor deviceCursor = devicesCollection.find();
		
		for(Document deviceDocument : deviceCursor) {
			deviceId = (String)deviceDocument.get("deviceId");
			deviceType = (String)deviceDocument.get("deviceType");
			dataUrl = (String)deviceDocument.get("dataUrl");
			dataType = (String)deviceDocument.get("dataType");
			read = (Boolean)deviceDocument.get("read");
			device = new Device(deviceId, deviceType, dataUrl, dataType, read);
			devices.add(device);
		}
		
		return devices;
	}
	
	private void removeData(Filter dataFilter) {
		NitriteCollection dataCollection = _db.getCollection(DATA_COLLECTION);
		dataCollection.remove(dataFilter);
		_logger.info("Removed data");
	}
	
	public String getAllDataInJson() {
		return getDataInJson(null);
	}
	
	public String getDataByDeviceIdInJson(String deviceIdToFilter) {
		Filter dataFilter = Filters.eq("deviceId", deviceIdToFilter);
		
		
		return getDataInJson(dataFilter);
	}
	
	public String getDataByTimestampInJson(String timestampToFilter) {
		Filter dataFilter = Filters.gte("timestamp", timestampToFilter);
		Filter dataToRemoveFilter = Filters.lte("timestamp", timestampToFilter);
		removeData(dataToRemoveFilter);
		
		return getDataInJson(dataFilter);
	}
	
	public String getDataByTimestampAndDeviceIdInJson(String deviceIdToFilter, String timestampToFilter) {
		Filter deviceFilter = Filters.eq("deviceId", deviceIdToFilter);
		Filter timestampFilter = Filters.gte("timestamp", timestampToFilter);
		Filter dataFilter = Filters.and(deviceFilter, timestampFilter);
		
		return getDataInJson(dataFilter);
	}
	
	private String getDataInJson(Filter dataFilter) {
		JSONObject jsonObject;
		JSONArray jsonArray = new JSONArray();
		String deviceId, dataUrl, dataType, parsedData, timestamp;
		NitriteCollection dataCollection = _db.getCollection(DATA_COLLECTION);
		Cursor dataCursor = (dataFilter != null) ? dataCollection.find(dataFilter) : dataCollection.find();
		
		for(Document dataDocument : dataCursor) {
			deviceId = (String)dataDocument.get("deviceId");
			dataUrl = (String)dataDocument.get("dataUrl");
			dataType = (String)dataDocument.get("dataType");
			parsedData = (String)dataDocument.get("parsedData");
			timestamp = (String)dataDocument.get("timestamp");
			
			jsonObject = getDataJsonObject(deviceId, dataUrl, dataType, parsedData, timestamp);
			jsonArray.put(jsonObject);
		}
		
		return jsonArray.toString();
	}
	
	private JSONObject getDataJsonObject(String deviceId, String dataUrl, String dataType, String parsedData, String timestamp) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("deviceId", deviceId);
		jsonObject.put("dataUrl", dataUrl);
		jsonObject.put("dataType", dataType);
		jsonObject.put("parsedData", parsedData);
		jsonObject.put("timestamp", timestamp);
		
		return jsonObject;
	}
	
	/* -------------------------------- INSERT METHODS -------------------------------- */
	public boolean insertDeviceDocument(Device device) {
		if(!hasDeviceInDb(device.getDeviceId())) {
			Document document = createDeviceDocument(device);
			NitriteCollection collection = _db.getCollection(DEVICES_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added device " + device.getDeviceId());
			
			return true;
		}
		
		_logger.warn("Device " + device.getDeviceId() + " already exists");
		return false;
	}
	
	/**
	 * It is used the device address, because we are not interested in getting the protocol
	 * and the adapter address
	 * 
	 * @param device
	 */
	/*public boolean insertDeviceDocument(DiscoveredDevice device) {
		String deviceUrl = device.getURL().getDeviceAddress();
		
		if(!hasDeviceInDb(deviceUrl)) {
			String deviceName = device.getName();
			Document document = createDeviceDocument(deviceName, deviceUrl);
			NitriteCollection collection = _db.getCollection(DEVICES_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added device " + deviceUrl);
			
			return true;
		} else {
			_logger.warn("Device " + deviceUrl + " already exists");
			return false;
		}
	}
	
	public boolean insertServiceDocument(GattService service) {
		String serviceUrl = service.getURL().getDeviceAddress() + "/" + service.getURL().getServiceUUID();
		
		if(!hasServiceInDb(serviceUrl)) {
			Document document = createServiceDocument(serviceUrl);
			NitriteCollection collection = _db.getCollection(SERVICES_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added service " + serviceUrl);
			
			return true;
		} else {
			_logger.warn("Service " + serviceUrl + " already exists");
			
			return false;
		}
	}
	
	/*public boolean insertCharacteristicDocument(String characteristicUrl, Characteristic characteristic) {
		if(!hasCharacteristicInDb(characteristicUrl)) {
			Document document = createCharacteristicDocument(characteristicUrl, characteristic);
			NitriteCollection collection = _db.getCollection(CHARACTERISTICS_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added characteristic " + characteristicUrl);
			
			return true;
		} else {
			_logger.warn("Characteristic " + characteristicUrl + " already exists");
			return false;
		}
	}
	
	public boolean insertFieldDocument(byte[] rawData, String fieldName, String characteristicUrl) {
		String fieldUrl = characteristicUrl + "/" + fieldName;
		
		if(!hasFieldInDb(fieldUrl)) {
			Document document = createFieldDocument(rawData, fieldName, fieldUrl);
			NitriteCollection collection = _db.getCollection(FIELDS_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added field " + fieldUrl);
			
			return true;
		} else {
			_logger.warn("Field " + fieldUrl + " already exists");
			return false;
		}
	}*/
	
	public void insertDataDocument(Device device, String parsedData) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
		Document document = createDataDocument(device, parsedData, timestamp);
		if(document != null) {
			NitriteCollection collection = _db.getCollection(DATA_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added data: [" + parsedData + ", " + device.getDeviceId() + "]");
		}
	}
	
	/*public void insertDataDocument(String parsedData, String fieldUrl) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		
		if(canAddNewData(timestamp, fieldUrl)) {
			Document document = createDataDocument(parsedData, fieldUrl, timestamp);
			NitriteCollection collection = _db.getCollection(DATA_COLLECTION);
			
			collection.insert(document);
			_logger.info("Added data: [" + parsedData + ", " + fieldUrl + "]");
		}
	}*/
	
	/* -------------------------------- UPDATE METHODS -------------------------------- */
	public boolean updateDeviceDocument(String deviceId, boolean read) {
		if(hasDeviceInDb(deviceId)) {
			NitriteCollection deviceCollection = _db.getCollection(DEVICES_COLLECTION);
			Cursor deviceCursor = deviceCollection.find(Filters.eq("deviceId", deviceId));

			if(deviceCursor.size() == 1) {
				for(Document deviceDocument : deviceCursor) {
					deviceDocument.put("read", read);
					deviceCollection.update(deviceDocument);
				}
				
				_logger.info("Updated device " + deviceId + " readability to " + read);
				return true;
			}
		}
		
		_logger.warn("Device " + deviceId + " does not exist");
		return false;
	}
	
	public void updateAllDeviceDocuments(boolean read) {
		NitriteCollection deviceCollection = _db.getCollection(DEVICES_COLLECTION);
		Cursor deviceCursor = deviceCollection.find();

		for(Document deviceDocument : deviceCursor) {
			deviceDocument.put("read", read);
			deviceCollection.update(deviceDocument);
		}
		
		_logger.info("Updated all devices' readability to " + read);
	}
	
	/* -------------------------------- CREATE METHODS -------------------------------- */
	private Document createDeviceDocument(Device device) {
		Document document = new Document();
		document.put("deviceId", device.getDeviceId());
		document.put("deviceType", device.getDeviceType());
		document.put("dataUrl", device.getDataUrl());
		document.put("dataType", device.getDataType());
		document.put("read", device.isNowReadable());
		
		return document;
	}
	
	/*private Document createDeviceDocument(String deviceName, String deviceUrl) {
		Document document = new Document();
		document.put("deviceName", deviceName);
		document.put("deviceUrl", deviceUrl);
		
		return document;
	}
	
	private Document createServiceDocument(String serviceUrl) {
		Document document = new Document();
		document.put("serviceUrl", serviceUrl);
		
		return document;
	}
	
	/*private Document createCharacteristicDocument(String characteristicUrl, Characteristic characteristic) {
		String characteristicName = characteristic.getName();
		boolean isReadable = characteristic.isValidForRead();
		boolean isWritable = characteristic.isValidForWrite();
		Document document = new Document();
		
		document.put("characteristicName", characteristicName);
		document.put("characteristicUrl", characteristicUrl);
		document.put("isReadable", isReadable);
		document.put("isWritable", isWritable);
		
		return document;
	}
	
	private Document createFieldDocument(byte[] rawData, String fieldName, String fieldUrl) {
		Document document = new Document();
		document.put("fieldName", fieldName);
		document.put("rawData", rawData);
		document.put("fieldUrl", fieldUrl);
		
		return document;
	}*/
	
	private Document createDataDocument(Device device, String parsedData, String timestamp) {
		if(canAddNewData(timestamp, device.getDataUrl())) {
			Document document = new Document();
			document.put("deviceId", device.getDeviceId());
			document.put("dataUrl", device.getDataUrl());
			document.put("dataType", device.getDataType());
			document.put("parsedData", parsedData);
			document.put("timestamp", timestamp);
			
			return document;
		}
		
		return null;
	}
	
	/*private Document createDataDocument(String parsedData, String fieldUrl, String timestamp) {
		Document document = new Document();
		document.put("parsedData", parsedData);
		document.put("fieldUrl", fieldUrl);
		document.put("timestamp", timestamp);
		
		return document;
	}*/

	/* ----------------------------- AUXILIARY DATA METHODS ---------------------------- */
	private boolean canAddNewData(String timestamp, String dataUrl) {
		if(dataUrl.equals("A0:9E:1A:2C:7C:A4/0000180d-0000-1000-8000-00805f9b34fb/00002a37-0000-1000-8000-00805f9b34fb/Heart Rate Measurement Value (uint8)")) {
			NitriteCollection dataCollection = _db.getCollection(DATA_COLLECTION);
			FindOptions dataFindOptions = FindOptions.sort("timestamp", SortOrder.Descending);
			Filter dataFilter = Filters.eq("dataUrl", dataUrl);
			Cursor dataCursor = dataCollection.find(dataFilter, dataFindOptions);
			
			if(dataCursor.size() == 0) { return true; }
			
			if(dataCursor.size() > 0) {
				Document dataDocument = dataCursor.firstOrDefault();
				String timestampToCompare = (String)dataDocument.get("timestamp");

                return hasNewData(timestamp, timestampToCompare);
			}
			
			return false;
		}
		
		return true;
	}
	
	// 0123456789012345678
	// yyyy-MM-dd HH:mm:ss
	// If its utilization is needed, this method needs to be adapted to the new date format
	// Checks if the new data received is not from the same minute to set one minute interval between
	// readings to be stored
	// yyyy-MM-dd'T'HH:mm:ss'Z'
	private boolean hasNewData(String timestamp1, String timestamp2) {
		int year1 = Integer.parseInt(timestamp1.substring(0, 4));
		int month1 = Integer.parseInt(timestamp1.substring(5, 7));
		int day1 = Integer.parseInt(timestamp1.substring(8, 10));
		int hour1 = Integer.parseInt(timestamp1.substring(11, 13));
		int minute1 = Integer.parseInt(timestamp1.substring(14, 16));
		int second1 = Integer.parseInt(timestamp1.substring(17, 19));
		
		int year2 = Integer.parseInt(timestamp2.substring(0, 4));
		int month2 = Integer.parseInt(timestamp2.substring(5, 7));
		int day2 = Integer.parseInt(timestamp2.substring(8, 10));
		int hour2 = Integer.parseInt(timestamp2.substring(11, 13));
		int minute2 = Integer.parseInt(timestamp2.substring(14, 16));
		int second2 = Integer.parseInt(timestamp2.substring(17, 19));
		
		//_logger.info("Timestamp1: " + year1 + "-" + month1 + "-" + day1 + " " + hour1 + ":" + minute1 + ":" + second1);
		//_logger.info("Timestamp2: " + year2 + "-" + month2 + "-" + day2 + " " + hour2 + ":" + minute2 + ":" + second2);
		
		if((year1 == year2) && (month1 == month2) && (day1 == day2) && (hour1 == hour2) && (minute1 == minute2) && (second1 == second2)) {
			return false;
		} else if((year1 == year2) && (month1 == month2) && (day1 == day2) && (hour1 == hour2) && (minute1 == minute2) && (second1 > second2 + 1)) {
			return true;
		} else if((year1 == year2) && (month1 == month2) && (day1 == day2) && (hour1 == hour2) && (minute1 > minute2)) {
			return true;
		} else if((year1 == year2) && (month1 == month2) && (day1 == day2) && (hour1 > hour2)) {
			return true;
		} else if((year1 == year2) && (month1 == month2) && (day1 > day2)) {
			return true;
		} else if((year1 == year2) && (month1 > month2)) {
			return true;
		} else return year1 > year2;

    }
	
	/* ------------------------------ HAS ELEMENT METHODS ------------------------------ */
	private boolean hasDeviceInDb(String deviceId) {
		return hasElementInDb(DEVICES_COLLECTION, "deviceId", deviceId);
	}
	
	/*private boolean hasDeviceInDb(String deviceUrl) {
		return hasElementInDb(DEVICES_COLLECTION, "deviceUrl", deviceUrl);
	}*/
	
	/*private boolean hasServiceInDb(String serviceUrl) {
		return hasElementInDb(SERVICES_COLLECTION, "serviceUrl", serviceUrl);
	}*/
	
	/*private boolean hasCharacteristicInDb(String characteristicUrl) {
		return hasElementInDb(CHARACTERISTICS_COLLECTION, "characteristicUrl", characteristicUrl);
	}
	
	private boolean hasFieldInDb(String fieldUrl) {
		return hasElementInDb(FIELDS_COLLECTION, "fieldUrl", fieldUrl);
	}*/
	
	private boolean hasElementInDb(String collectionName, String fieldToEvaluate, String elementUrl) {
		NitriteCollection collection = _db.getCollection(collectionName);
		Cursor cursor = collection.find(Filters.eq(fieldToEvaluate, elementUrl));
		
		return (cursor.size() > 0);
	}

	/* ---------------------------- BIND ELEMENTS METHODS ----------------------------- */
	/**
	 * The regular expression in the serviceFilter looks for serviceUrl which contains the
	 * deviceUrl in its beginning
	 * 
	 * @param deviceUrl
	 */
	/*public void addServicesToDevice(String deviceUrl) {
		if(hasDeviceInDb(deviceUrl)) {
			NitriteCollection deviceCollection = _db.getCollection(DEVICES_COLLECTION);
			Cursor deviceCursor = deviceCollection.find(Filters.eq("deviceUrl", deviceUrl));

			if(deviceCursor.size() == 1) {
				NitriteCollection serviceCollection = _db.getCollection(SERVICES_COLLECTION);
				Filter serviceFilter = Filters.regex("serviceUrl", "^(" + deviceUrl + ").*");
				Cursor serviceCursor = serviceCollection.find(serviceFilter);
				ArrayList<Long> services = new ArrayList<Long>();
				
				for(Document serviceDocument : serviceCursor) {
					Long serviceId = new Long((long)serviceDocument.get("_id"));
					services.add(serviceId);
				}
				
				Filter deviceFilter = Filters.eq("deviceUrl", deviceUrl);
				Document documentToUpdate = Document.createDocument("services", services);
				
				deviceCollection.update(deviceFilter, documentToUpdate);
				
				_logger.info("Services added to the device " + deviceUrl);
			} else {
				_logger.error("There is more than one device in DB with " + deviceUrl);
			}
		} else {
			_logger.warn("The device " + deviceUrl + " doesn't exist, so it is impossible to add services to it");
		}
	}

	/**
	 * The regular expression in the characteristicFilter looks for characteristicUrlUrl 
	 * which contains the serviceUrl in its beginning
	 * 
	 * @param serviceUrl
	 */
	/*public void addCharacteristicsToService(String serviceUrl) {
		if(hasServiceInDb(serviceUrl)) {
			NitriteCollection serviceCollection = _db.getCollection(SERVICES_COLLECTION);
			Cursor serviceCursor = serviceCollection.find(Filters.eq("serviceUrl", serviceUrl));

			if(serviceCursor.size() == 1) {
				NitriteCollection characteristicCollection = _db.getCollection(CHARACTERISTICS_COLLECTION);
				Filter characteristicFilter = Filters.regex("characteristicUrl", "^(" + serviceUrl + ").*");
				Cursor characteristicCursor = characteristicCollection.find(characteristicFilter);
				ArrayList<Long> characteristics = new ArrayList<Long>();
				
				for(Document characteristicDocument : characteristicCursor) {
					Long characteristicId = new Long((long)characteristicDocument.get("_id"));
					characteristics.add(characteristicId);
				}
				
				Filter serviceFilter = Filters.eq("serviceUrl", serviceUrl);
				Document documentToUpdate = Document.createDocument("characteristics", characteristics);
				
				serviceCollection.update(serviceFilter, documentToUpdate);
				
				_logger.info("Characteristics added to the service " + serviceUrl);
			} else {
				_logger.error("There is more than one service in DB with " + serviceUrl);
			}
		} else {
			_logger.warn("The service " + serviceUrl + " doesn't exist, so it is impossible to add characteristics to it");
		}
	}
	
	public void addFieldsToCharacteristic(String characteristicUrl) {
		if(hasCharacteristicInDb(characteristicUrl)) {
			NitriteCollection characteristicCollection = _db.getCollection(CHARACTERISTICS_COLLECTION);
			Cursor characteristicCursor = characteristicCollection.find(Filters.eq("characteristicUrl", characteristicUrl));

			if(characteristicCursor.size() == 1) {
				NitriteCollection fieldCollection = _db.getCollection(FIELDS_COLLECTION);
				Filter fieldFilter = Filters.regex("fieldUrl", "^(" + characteristicUrl + ").*");
				Cursor fieldCursor = fieldCollection.find(fieldFilter);
				ArrayList<Long> fields = new ArrayList<Long>();
				
				for(Document fieldDocument : fieldCursor) {
					Long fieldId = new Long((long)fieldDocument.get("_id"));
					fields.add(fieldId);
				}
				
				Filter characteristicFilter = Filters.eq("characteristicUrl", characteristicUrl);
				Document documentToUpdate = Document.createDocument("fields", fields);
				
				characteristicCollection.update(characteristicFilter, documentToUpdate);
				
				_logger.info("Fields added to the characteristic " + characteristicUrl);
			} else {
				_logger.error("There is more than one characteristic in DB with " + characteristicUrl);
			}
		} else {
			_logger.warn("The characteristic " + characteristicUrl + " doesn't exist, so it is impossible to add fields to it");
		}
	}
	
	public void addDataToField(String fieldUrl) {
		if(hasFieldInDb(fieldUrl)) {
			NitriteCollection fieldCollection = _db.getCollection(FIELDS_COLLECTION);
			Cursor fieldCursor = fieldCollection.find(Filters.eq("fieldUrl", fieldUrl));

			if(fieldCursor.size() == 1) {
				NitriteCollection dataCollection = _db.getCollection(DATA_COLLECTION);
				Filter dataFilter = Filters.eq("fieldUrl", fieldUrl);
				Cursor dataCursor = dataCollection.find(dataFilter);
				ArrayList<Long> data = new ArrayList<Long>();
				
				for(Document dataDocument : dataCursor) {
					Long dataId = new Long((long)dataDocument.get("_id"));
					data.add(dataId);
				}
				
				Filter fieldFilter = Filters.eq("fieldUrl", fieldUrl);
				Document documentToUpdate = Document.createDocument("data", data);
				
				fieldCollection.update(fieldFilter, documentToUpdate);
				
				_logger.info("Data added to the field " + fieldUrl);
			} else {
				_logger.error("There is more than one field in DB with " + fieldUrl);
			}
		} else {
			_logger.warn("The field " + fieldUrl + " doesn't exist, so it is impossible to add data to it");
		}
	}*/
}
