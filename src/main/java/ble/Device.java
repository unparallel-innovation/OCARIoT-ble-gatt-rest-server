package ble;

public class Device {

	public static final String BAND = "Band";
	public static final String SENSOR = "Sensor";
	public static final String HEART_RATE = "HeartRate";
	public static final String TEMPERATURE = "Temperature";
	public static final String HUMIDITY = "Humidity";
	public static final String WEIGHT = "Weight";

	
	private String _deviceId, _deviceType, _dataUrl, _dataType;
	private boolean _readabilityStatus;
	
	public Device(String deviceId, String deviceType, String dataUrl, String dataType, boolean readabilityStatus) {
		_deviceId = deviceId;
		_deviceType = deviceType;
		_dataUrl = dataUrl;
		_dataType = dataType;
		_readabilityStatus = readabilityStatus;	
	}
	
	public String getDeviceId() {
		return _deviceId;
	}
	
	public String getDeviceType() {
		return _deviceType;
	}
	
	public String getDataUrl() {
		return _dataUrl;
	}
	
	public String getDataType() {
		return _dataType;
	}
	
	public void setReadabilityStatus(boolean readabilityStatus) {
		_readabilityStatus = readabilityStatus;
	}
	
	public boolean isNowReadable() {
		return _readabilityStatus;
	}
}
