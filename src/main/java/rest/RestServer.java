package rest;

import ble.BlutoothBeurer;
import org.apache.log4j.Logger;

import ble.Device;
import ble.BleManager;
import org.json.JSONObject;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import spark.Spark;
import storage.DbOperations;

public class RestServer {
	
	private final Logger _logger;
	private DbOperations _db;
	private BleManager bleManager;
	
	public RestServer(int portToListen, BleManager _bleManager) {
		_logger = Logger.getLogger(RestServer.class);
		_db = BleManager.getDb();
		bleManager=_bleManager;

		if(portToListen != -1) { Spark.port(portToListen); }
		//Spark.ipAddress("10.0.1.15");
		setServerRoutes();
	}
	
	private void setServerRoutes() {
		setGetRoutes();
		//setPostRoutes();
		setPutRoutes();
	}
	
	private void setGetRoutes() {
		Spark.get("/values", (request, response) -> {
			String timestamp = request.queryParams("since");
			String deviceId = request.queryParams("id");
			String dataToSend;
			
			if(request.queryParams().size() == 0) {
				_logger.info("Listing all data");
				dataToSend = _db.getAllDataInJson();
			} else if((deviceId != null) && (timestamp != null)) {
				_logger.info("Listing id & since: " + deviceId + " & " + timestamp);
				dataToSend = _db.getDataByTimestampAndDeviceIdInJson(deviceId, timestamp);
			} else if(timestamp != null) {
				_logger.info("Listing since: " + timestamp);
				dataToSend = _db.getDataByTimestampInJson(timestamp);
			} else if(deviceId != null) {
				_logger.info("Listing id: " + deviceId);
				dataToSend = _db.getDataByDeviceIdInJson(deviceId);
			} else {
				response.status(400);
				return sendRequestError("Invalid GET Request!", "Valid GET requests are \"/values\", \"/values?since=\", \"/values?id=\" and \"/values?id=&since=\"");
			}
			
			response.status(200);
			return dataToSend;
		});

		Spark.get("/weight", (request, response) -> {

			float weight= bleManager.getWeight();
			bleManager.resetWeight();
			JSONObject dataToSend;


			if (weight!=-1) {

				response.status(200);
				response.type("application/json");
				dataToSend = new JSONObject("{ weight:"+weight+"}");
				return dataToSend;

			}else {
				response.status(500);
				return ("There is no new weight");

			}


		});

		Spark.get("/ontop", (request, response) -> {

			boolean onTop = bleManager.getPersonOnTop();



			if (onTop) {
				response.status(200);
				return "Someone on top of the scale!";

			}else {
				response.status(500);
				return ("No one on top of the scale!");

			}


		});

	}
	
/*	private void setGetRoutes() {
		Spark.get("/values", (request, response) -> {
			switch(request.queryParams().size()) {
				case 0: {
					_logger.info("Listing data");
					return _db.getAllDataInJson();
				}
				case 1: {
					if(request.queryParams("since") != null) {
						_logger.info("Listing since: " + request.queryParams("since"));
						return _db.getDataByTimestampInJson(request.queryParams("since"));
					} else if(request.queryParams("id") != null) {
						_logger.info("Listing id: " + request.queryParams("id"));
						return _db.getDataByBandIdInJson(request.queryParams("id"));
					}
					break;
				}
				case 2: {
					if((request.queryParams("since") != null) && (request.queryParams("id") != null)) {
						_logger.info("Listing since & id: " + request.queryParams("since") + " & " + request.queryParams("id"));
						return _db.getDataByTimestampAndBandIdInJson(request.queryParams("id"), request.queryParams("since"));
					}
					break;
				}
				default: break;
			}
			
			response.status(400);
			return sendRequestError("Invalid GET Request!", "Valid GET requests are \"/values\", \"/values?since=\", \"/values?id=\" and \"/values?id=&since=\"");
		});
	}*/
	
/*	private void setPostRoutes() {
		Spark.post("/subscriptions", (request, response) -> {
			if((request.queryParams("id") != null) && (request.queryParams("url") != null)) {
				String bandId = request.queryParams("id");
				String dataUrl = request.queryParams("url");
				Device band = new Device(bandId, dataUrl, false);
				
				if(_db.insertBandDocument(band)) {
					response.status(201);
					return ("Added band " + band.getDeviceId());
				} else {
					return "Band already exists";
				}
			}
			
			response.status(400);
			return sendRequestError("Invalid POST Request!", "Valid POST request is \"/subscriptions?id=&url=\"");
		});
	}*/
	
	private void setPutRoutes() {
		Spark.put("/subscriptions", (request, response) -> {
			String deviceId = request.queryParams("id");
			String readParam = request.queryParams("read");
			
			if(readParam != null) {
				if(!readParam.equals("true") && !readParam.equals("false")) {
					response.status(400);
					return sendRequestError("Invalid Read Option!", "");
				}
				
				boolean read = Boolean.parseBoolean(readParam);
				
				if(deviceId != null) {
					if(_db.updateDeviceDocument(deviceId, read)) {
						response.status(201);
						return ("Updated device " + deviceId + " readability to " + read + "\n");
					}
				} else {
					_db.updateAllDeviceDocuments(read);
					response.status(201);
					return ("Updated all devices' readability to " + read + "\n");
				}
			}
			
/*			if((bandId != null) && (readParam != null)) {
				if(readParam != "true" && readParam != "false") {
					response.status(400);
					return sendRequestError("Invalid Read Option!", "");
				}
				
				boolean read = Boolean.parseBoolean(readParam);
				if(_db.updateBandDocument(bandId, read)) {
					response.status(201);
					return ("Updated band " + bandId);
				}
			}*/
			
			response.status(400);
			return sendRequestError("Invalid PUT Request!", "Valid PUT requests are \"/subscriptions?id=&read=\" and \"/subscriptions?read=\"");
		});
		Spark.put("/initscale", (request, response)-> {

			System.out.println("Recebido initscale PUT");

			bleManager.setScaleStandbyFalse();
			boolean status= bleManager.getScaleInitialized();

			System.out.println("Status= " + status);



			if (status==true) {

				response.status(200);
				return ("Scale initialized");

			}else {
				response.status(400);
				return ("Scale not initialized");

			}


		});

		Spark.put("/standby", (request, response)-> {

			System.out.println("Recebido standby PUT");


			bleManager.setScaleStandbyTrue();
			bleManager.setScaleNotInitialized();

			response.status(200);
			return ("Standby");


		});

		Spark.put("/reset", (request, response)-> {

				bleManager.resetWeight();
				bleManager.setPersonOnTop(false);
				response.status(200);
				return ("Reset");

		});
	}
	
	private String sendRequestError(String error, String validRequest) {
		_logger.info(error);
		return error + "\n" + validRequest + "\n";
	}
}
