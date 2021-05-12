package ble;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.manager.*;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;
import utils.OSInfo;
import utils.Print;
import utils.ScaleUtils;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static utils.ScaleUtils.toInt32Be;

public class BlutoothBeurer {


    private BluetoothManagerBuilder bleManagerBuilder;
    private final BluetoothManager bluetoothManager;
    private OSInfo _osInfo;

    private final  int ID_START_NIBBLE_INIT = 6;
    private final  int ID_START_NIBBLE_CMD = 7;
    private byte startByte = (byte) (0xf0 | ID_START_NIBBLE_CMD);
    private final  int ID_START_NIBBLE_SET_TIME = 9;
    private final  byte CMD_SCALE_STATUS = (byte)0x4f;
    private final byte CMD_SET_UNIT = (byte)0x4d;
    private long unixTime;
    private final byte CMD_SCALE_ACK = (byte)0xf0;

    private boolean scaleInitialized=false;
    private float weight=-1;
    private boolean found;
    private boolean stopped;
    private int stepNr;

    CharacteristicGovernor characteristicGovernor;

    private final String serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private final String characteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private String deviceUrl;
    private String dataUrl;
    private URL characteristicUrl;


    public BlutoothBeurer(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {

        /*_osInfo = new OSInfo();
        buildBleManager(start, activateDiscovering, discoveryRate, refreshRate, rediscover);*/

        bluetoothManager = new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .withIgnoreTransportInitErrors(true)
                .withDiscovering(activateDiscovering)
                .withRediscover(rediscover)
                .withStarted(start)
                .build();

    }

    /*public void buildBleManager(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {
        setBleManagerConfiguration(start, activateDiscovering, discoveryRate, refreshRate, rediscover);
        bluetoothManager = bleManagerBuilder.build();
    }*/

    /*private void setBleManagerConfiguration(boolean start, boolean activateDiscovering, int discoveryRate, int refreshRate, boolean rediscover) {
        bleManagerBuilder = new BluetoothManagerBuilder();

        bleManagerBuilder.withStarted(start);
        bleManagerBuilder.withDiscovering(activateDiscovering);
        bleManagerBuilder.withIgnoreTransportInitErrors(true);
        setBleManagerTransportType();
        setBleManagerDiscoveryConfiguration(discoveryRate, refreshRate, rediscover);
    }*/

    private void setBleManagerTransportType() {
        int osType = getOsType();

        switch(osType) {
            case OSInfo.LINUX: {
                bleManagerBuilder.withTinyBTransport(true);
                //_bleManagerBuilder.withBlueGigaTransport("(/dev/ttyACM)[0-9]{1,3}");
                break;
            }
            case OSInfo.WINDOWS: {
                bleManagerBuilder.withBlueGigaTransport("(COM)[0-9]{1,3}");
                break;
            }
            case OSInfo.MACOS: {
                bleManagerBuilder.withBlueGigaTransport("/dev/tty.(usbmodem).*");
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

    /*private void setBleManagerDiscoveryConfiguration(int discoveryRate, int refreshRate, boolean rediscover) {
        if(discoveryRate != -1) {
            bleManagerBuilder.withDiscoveryRate(discoveryRate);
        }

        if(refreshRate != -1) {
            bleManagerBuilder.withRefreshRate(refreshRate);
        }

        bleManagerBuilder.withRediscover(rediscover);

        //_logger.info("Discovery configuration was set");
    }


    /*private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            .withDiscovering(true)
            .withRediscover(true)
            .build();*/

    private String getAdapterUrl() {
        ArrayList<DiscoveredAdapter> adapters = new ArrayList<DiscoveredAdapter>(bluetoothManager.getDiscoveredAdapters());
        DiscoveredAdapter adapter = adapters.get(0);
        String protocolAndAdapterUrl = adapter.getURL().toString();
        String adapterUrl = protocolAndAdapterUrl.substring(protocolAndAdapterUrl.indexOf("/"));

        return adapterUrl;
    }

    private byte getAlternativeStartByte(int startNibble) {
        return (byte) ((startByte & 0xF0) | startNibble);
    }

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

    public synchronized boolean getScaleInitialized(){

        return this.scaleInitialized;

    }


    boolean personOnTop=false;

    public synchronized boolean getPersonOnTop(){

        return this.personOnTop;

    }

    public synchronized void setPersonOnTop(boolean status){

        personOnTop=status;

    }


    protected synchronized void stopMachineState() {
        System.out.println("Stop machine state");
        stopped = true;
    }

    public synchronized void resumeMachineState() {
        System.out.println("Resume machine state");
        stopped = false;
        nextMachineStep();
    }

    public synchronized void jumpNextToStepNr(int nr) {
        System.out.println("Jump next to step nr");
        stepNr = nr;
    }

    private synchronized void nextMachineStep() {
        if (!stopped) {
            System.out.println("Step Nr " + stepNr);
            if (onNextStep(stepNr)) {
                stepNr++;
                nextMachineStep();
            } else {
                System.out.println("Ended Machine State");
                stopMachineState();

            }
        }
    }

    private void enableDeviceConnection(String adapterUrl, String dataUrl) {
        String deviceAddress = dataUrl.substring(0, dataUrl.indexOf("/"));
        URL deviceUrl = new URL(adapterUrl + "/" + deviceAddress);
        bluetoothManager.getDeviceGovernor(deviceUrl, true);
        System.out.println("Connection enabled to device " + deviceUrl);

    }

    protected boolean onNextStep(int stepNr) {

        byte[] command;

        switch (stepNr) {

            case 0:

                //Add device listener and discover device
                bluetoothManager.addDeviceDiscoveryListener(this::discoverBeurer);

                stopMachineState();
                break;

            case 1:

                stopMachineState();

                String adapterUrl = getAdapterUrl();
                enableDeviceConnection(adapterUrl, dataUrl);
                URL characteristicUrl = new URL(adapterUrl + "/" + dataUrl.substring(0, dataUrl.lastIndexOf("/")));
                System.out.println(characteristicUrl);

                characteristicGovernor = bluetoothManager.getCharacteristicGovernor(characteristicUrl);
                bluetoothManager.getDeviceGovernor(characteristicUrl).setConnectionControl(true);
                characteristicGovernor.whenReady(CharacteristicGovernor::isNotifiable).thenAccept(this::notificationReady);


                break;

            case 2:
                //Add device data listener
                characteristicGovernor.addValueListener(this::dataProcess);
                break;

            case 3:
                // Say "Hello" to the scale and wait for ack
                command = ScaleUtils.sendAlternativeStartCode(ID_START_NIBBLE_INIT, (byte) 0x01);
                characteristicGovernor.write(command);
                stopMachineState();
                break;

            case 4:
                // Update time on the scale (no ack)
                this.unixTime = System.currentTimeMillis() / 1000L;
                command = ScaleUtils.sendAlternativeStartCode(ID_START_NIBBLE_SET_TIME, toInt32Be(unixTime));
                characteristicGovernor.write(command);
                break;

            case 5:
                // Request scale status and wait for ack
                command = ScaleUtils.sendCommand(CMD_SCALE_STATUS, ScaleUtils.encodeUserId(null));
                characteristicGovernor.write(command);
                stopMachineState();
                break;


            default:
                // Finish init if everything is done
                return false;
        }

        return true;

    }




    private void discoverBeurer(DiscoveredDevice discoveredDevice) {

        if(discoveredDevice.getDisplayName().equals("Beurer BF700")) {

            String fieldName = "Weight Measurement Value (Kg)";
            String deviceU = discoveredDevice.getURL().toString().replace("tinyb:/XX:XX:XX:XX:XX:XX/","");
            String dataU = deviceU + "/" + serviceUuid + "/" + characteristicUuid + "/" + fieldName;

            System.out.println("Name: "+discoveredDevice.getDisplayName()+" Device: " + deviceU + " DataURL: " + dataU);
            processDiscovery(deviceU,dataU);

        }

    }

    private void processDiscovery(String deviceU, String dataU){

        this.deviceUrl = deviceU;
        this.dataUrl = dataU;
        resumeMachineState();
    }

    private void notificationReady(boolean notifiable) {

        if(notifiable) {
            resumeMachineState();
        }

    }

    private void dataProcess(byte[] data) {

        System.out.println(new String(data, StandardCharsets.UTF_8));

        if (data == null || data.length == 0) {
            return;
        }

        if (data[0] == getAlternativeStartByte(ID_START_NIBBLE_INIT)) {

            System.out.println("Got init ack from scale, Scale is ready");
            resumeMachineState();
            return;
        }

        if (data[0] != startByte) {
            System.out.println("Got unknown start byte");
            return;
        }

        try {
            switch (data[1]) {


                case CMD_SCALE_ACK:
                    processScaleAck(data);
                    break;

                case 88:

                    setPersonOnTop(true);
                    boolean stableMeasurement = data[2] == 0;
                    float weight = ScaleUtils.getKiloGram(data, 3);
                    System.out.println("Person on top: "+ personOnTop + " Stable: " + stableMeasurement + " Weight:  " + weight);

                    if (stableMeasurement) {
                        setWeight(weight);
                        setPersonOnTop(false);
                    }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processScaleAck(byte[] data){

        System.out.println(new String(data, StandardCharsets.UTF_8));

        byte[] command;

        switch (data[2]) {
            case CMD_SCALE_STATUS:

                final int currentUnit = data[7] & 0xFF;
                final int requestedUnit = (byte) 1;

                if (requestedUnit != currentUnit) {
                    command = ScaleUtils.sendCommand(CMD_SET_UNIT, (byte) 1);
                    characteristicGovernor.write(command);
                }
                else  {
                    resumeMachineState();
                }
                break;

            case CMD_SET_UNIT:
                if (data[3] == 0) {
                    System.out.println("Scale unit successfully set");
                }
                resumeMachineState();
                break;


        }

    }



}
