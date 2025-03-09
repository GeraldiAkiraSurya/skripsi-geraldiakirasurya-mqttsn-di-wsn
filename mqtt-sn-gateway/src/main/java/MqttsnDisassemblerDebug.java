public class MqttsnDisassemblerDebug {

    public byte[] removeEncapsulation(byte[] encapsulatedMessage) {
        int length = encapsulatedMessage[0] & 0xFF;

        int mqttsnLength;
        if (encapsulatedMessage[length] == 0x01) {
            mqttsnLength = (encapsulatedMessage[length+1] & 0xFF) << 8 | (encapsulatedMessage[length+2] & 0xFF);
        } else {
            mqttsnLength = encapsulatedMessage[length];
        }

        byte[] mqttsnMessage = new byte[mqttsnLength];
        System.arraycopy(encapsulatedMessage, length, mqttsnMessage, 0, mqttsnLength);

        return mqttsnMessage;
    }

    public byte[] getMessageHeader(byte[] message) {
        byte[] messageHeader = new byte[0];
        if (message[0] != (byte) 0x01) {
            messageHeader = new byte[2];
            System.arraycopy(message, 0, messageHeader, 0, messageHeader.length);
        } else if (message[0] == (byte) 0x01) {
            messageHeader = new byte[4];
            System.arraycopy(message, 0, messageHeader, 0, messageHeader.length);
        }

        return messageHeader;
    }

    public byte[] getMessageVariablePart(byte[] message) {
        byte[] messageVariablePart = new byte[0];
        if (message[0] != (byte) 0x01) {
            int length = message[0] & 0xFF;
            messageVariablePart = new byte[length-2];
            System.arraycopy(message, 2, messageVariablePart, 0, length-2);
        } else if (message[0] == (byte) 0x01) {
            int length = ((message[1] & 0xFF) << 8) | (message[2] & 0xFF);
            messageVariablePart = new byte[length-4];
            System.arraycopy(message, 4, messageVariablePart, 0, length-4);
        }

        return messageVariablePart;
    }

    public byte getMessageType(byte[] message) {
        byte messageType = (byte) 0x00;
        if (message[0] != (byte) 0x01) {
            messageType = message[1];
        } else if (message[0] == (byte) 0x01) {
            messageType = message[2];
        }

        return messageType;
    }
}
