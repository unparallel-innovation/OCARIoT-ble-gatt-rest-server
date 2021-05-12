package utils;

/* Code based on OPEN Scale application as can be seen in https://github.com/oliexdev/openScale*/

public class ScaleUtils {

    private final static int ID_START_NIBBLE_CMD = 7;
    private final static int ID_START_NIBBLE_SET_TIME = 9;



    private final static byte CMD_SCALE_STATUS = (byte)0x4f;

    private final static byte CMD_USER_ADD = (byte)0x31;
    private final static byte CMD_USER_DELETE = (byte)0x32;
    private final static byte CMD_USER_LIST = (byte)0x33;


    private final static byte CMD_DO_MEASUREMENT = (byte)0x40;


    private final static byte CMD_GET_UNKNOWN_MEASUREMENTS = (byte)0x46;
    private final static byte CMD_DELETE_UNKNOWN_MEASUREMENT = (byte)0x49;

    private static byte startByte = (byte) (0xf0 | ID_START_NIBBLE_CMD);

    protected static class RemoteUser {
        final public long remoteUserId;
        final public String name;
        final public int year;

        public int localUserId = -1;
        public boolean isNew = false;

        RemoteUser(long uid, String name, int year) {
            this.remoteUserId = uid;
            this.name = name;
            this.year = year;
        }
    }

    private static byte getAlternativeStartByte(int startNibble) {
        return (byte) ((startByte & 0xF0) | startNibble);
    }

    public static byte[] sendAlternativeStartCode(int id, byte... parameters) {
        byte[] data = new byte[parameters.length + 1];
        data[0] = getAlternativeStartByte(id);

        int i = 1;
        for (byte parameter : parameters) {
            data[i++] = parameter;
        }

        return data;
    }

    public static void toInt32Be(byte[] data, int offset, long value) {
        data[offset + 0] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    public static byte[] toInt32Be(long value) {
        byte[] data = new byte[4];
        toInt32Be(data, 0, value);
        return data;
    }

    public static byte[] sendCommand(byte command, byte... parameters) {
        byte[] data = new byte[parameters.length + 2];
        data[0] = startByte;
        data[1] = command;

        int i = 2;
        for (byte parameter : parameters) {
            data[i++] = parameter;
        }

        return data;
    }

    public static byte[] encodeUserId(RemoteUser remoteUser) {
        long uid = remoteUser != null ? remoteUser.remoteUserId : 0;
        byte[] data = new byte[8];
        toInt32Be(data, 0, uid >> 32);
        toInt32Be(data, 4, uid & 0xFFFFFFFF);
        return data;
    }

    public static int fromSignedInt16Be(byte[] data, int offset) {
        int value = data[offset] << 8;
        value += data[offset + 1] & 0xFF;
        return value;
    }

    public static int fromUnsignedInt16Be(byte[] data, int offset) {

        return fromSignedInt16Be(data, offset) & 0xFFFF;
    }

    public static float getKiloGram(byte[] data, int offset) {
        // Unit is 50 g
        return fromUnsignedInt16Be(data, offset) * 50.0f / 1000.0f;
    }

}
