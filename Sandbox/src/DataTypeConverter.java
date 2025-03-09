public class DataTypeConverter {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];

        int v = b & 0xFF;
        hexChars[0] = HEX_ARRAY[v >>> 4];
        hexChars[1] = HEX_ARRAY[v & 0x0F];

        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] intToTwoByteArray(int x) {
        byte [] data = new byte [2];

        data[0] = (byte) ((x & 0xFF00) >>> 8);
        data[1] = (byte) (x & 0x00FF);
        return data;
    }
}
