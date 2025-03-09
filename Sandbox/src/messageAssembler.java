/*

public class messageAssembler {
//    public byte[] messageHeader;
//    public byte[] messageVariablePart;
//    public byte[] message;

    public messageAssembler() {
//        this.messageHeader = new byte[0];
//        this.messageVariablePart = new byte[0];
//        this.message = new byte[0];
    }

//    public void createMessage(byte[] message) {
//        this.message = message;
//
//        int length = message[0];
//        if (length != 01) {
//
//        }
//    }

    public MQTTSNMessageOld createSEARCHGW(int radius) {
        byte messageType = 0x01;
        byte[] messageVariablePart = new byte[] {(byte) radius}; // radius

        MQTTSNMessageOld mqttsnMessage = new MQTTSNMessageOld();
        mqttsnMessage.createMessage(messageType, messageVariablePart);

//        byte[] lengthField = createLengthField();
////        System.out.println(DataTypeConverter.bytesToHex(lengthField));
//
//        messageHeader = new byte[lengthField.length + 1];
//        System.arraycopy(lengthField, 0, messageHeader, 0, lengthField.length);
//        messageHeader[lengthField.length] = messageType;
//        //System.out.println("Header field length\t: " + messageHeader.length);
//
//        message = new byte[messageHeader.length + messageVariablePart.length];
//        //System.out.println("Total Pakcet length\t: " + message.length);
//        System.arraycopy(messageHeader, 0, message, 0, messageHeader.length);
//        System.arraycopy(messageVariablePart, 0, message, messageHeader.length, messageVariablePart.length);

        return mqttsnMessage;
    }

    public MQTTSNMessageOld createGWINFO(byte gwId) {
        byte messageType = 0x02;
        byte[] messageVariablePart = new byte[] {gwId}; // radius

        MQTTSNMessageOld mqttsnMessage =  new MQTTSNMessageOld();
        mqttsnMessage.createMessage(messageType, messageVariablePart);

//        byte[] lengthField = createLengthField();
////        System.out.println(DataTypeConverter.bytesToHex(lengthField));
//
//        messageHeader = new byte[lengthField.length + 1];
//        System.arraycopy(lengthField, 0, messageHeader, 0, lengthField.length);
//        messageHeader[lengthField.length] = messageType;
//        //System.out.println("Header field length\t: " + messageHeader.length);
//
//        message = new byte[messageHeader.length + messageVariablePart.length];
//        //System.out.println("Total Pakcet length\t: " + message.length);
//        System.arraycopy(messageHeader, 0, message, 0, messageHeader.length);
//        System.arraycopy(messageVariablePart, 0, message, messageHeader.length, messageVariablePart.length);

        return mqttsnMessage;
    }

//    private byte[] createLengthField() {
//        byte[] lengthField = new byte[0];
//        if (messageVariablePart.length > 253) {
//
//        } else {
//            int totalPacketLength = 1 + 1 + messageVariablePart.length;
//            //System.out.println("Payload Field length\t: " + messageVariablePart.length);
//            lengthField = new byte[] {(byte) totalPacketLength};
//        }
//
//        return lengthField;
//    }
}

 */