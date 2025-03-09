public class MqttsnAssembler {
    public MqttsnAssembler() {}

    public byte[] encapsulateMessage(byte[] wirelessNodeId, byte[] mqttsnMessage) {
        byte messageType = (byte) 0xFE;
        byte ctrl = (byte) 0x00;

        byte[] messageVariablePart = new byte[wirelessNodeId.length + 1];
        messageVariablePart[0] = ctrl;
        System.arraycopy(wirelessNodeId, 0, messageVariablePart, 1, wirelessNodeId.length);

        byte[] envelope = assembleMessage(messageType, messageVariablePart);

        byte[] finalMessage = new byte[envelope.length + mqttsnMessage.length];
        System.arraycopy(envelope, 0 , finalMessage, 0, envelope.length);
        System.arraycopy(mqttsnMessage, 0 , finalMessage, envelope.length, mqttsnMessage.length);

        return finalMessage;
    }

    public byte[] createSEARCHGW(int radius) {
        byte messageType = (byte) 0x01;
        byte[] messageVariablePart = new byte[] {(byte) radius};

        return assembleMessage(messageType, messageVariablePart);
    }

    public byte[] createGWINFO(byte gwId) {
        byte messageType = (byte) 0x02;
        byte[] messageVariablePart = new byte[] {gwId};

        return assembleMessage(messageType, messageVariablePart);
    }

    public byte[] createCONNECT(boolean will, boolean cleanSession, String clientId) {
        byte messageType = (byte) 0x04;
        byte flags = (byte) 0b00000000;

        if (will) {
            flags = (byte) (flags | (1 << 3));
        }
        if (cleanSession) {
            flags = (byte) (flags | (1 << 2));
        }

        byte protocolId = (byte) 0x01;

        int tempDuration = 300; // Max 65535 || 0xFFFF
        byte[] duration = DataTypeConverter.intToTwoByteArray(tempDuration);

        byte[] newClientId = clientId.getBytes();

        byte[] messageVariablePart = new byte[1+1+2+newClientId.length];
        messageVariablePart[0] = flags;
        messageVariablePart[1] = protocolId;
        System.arraycopy(duration, 0, messageVariablePart, 2, duration.length);
        System.arraycopy(newClientId, 0, messageVariablePart, 4, newClientId.length);

        return assembleMessage(messageType, messageVariablePart);
    }

    public byte[] createCONNACK(byte returnCode) {
        byte messageType = (byte) 0x05;
        byte[] messageVariablePart = new byte[] {returnCode};

        return assembleMessage(messageType, messageVariablePart);
    }

    public byte[] createPUBLISH(byte dup, byte qos, byte retain, byte[] topicIdType, byte[] topicId, String data) {
        byte messageType = (byte) 0x0C;
        byte flags = (byte) 0b00000000;
        byte[] messageId = {0x00, 0x00};

        byte[] newData = data.getBytes();
        byte[] messageVariablePart = new byte[1+topicId.length+messageId.length+ newData.length];
        messageVariablePart[0] = flags;
        messageVariablePart[1] = topicId[0];
        messageVariablePart[2] = topicId[1];
        messageVariablePart[3] = messageId[0];
        messageVariablePart[4] = messageId[1];
        System.arraycopy(newData, 0, messageVariablePart, 5, newData.length);

        return assembleMessage(messageType, messageVariablePart);
    }

    private byte[] assembleMessage(byte msgType, byte[] msgVariablePart) {
        byte[] messageLength = createLengthField(msgVariablePart);

        byte[] messageHeader = new byte[messageLength.length + 1];
        System.arraycopy(messageLength, 0, messageHeader, 0, messageLength.length);
        messageHeader[messageLength.length] = msgType;

        byte[] finalMessage = new byte[messageHeader.length + msgVariablePart.length];
        System.arraycopy(messageHeader, 0, finalMessage, 0, messageHeader.length);
        System.arraycopy(msgVariablePart, 0, finalMessage, messageHeader.length, msgVariablePart.length);

        return finalMessage;
    }

    private byte[] createLengthField(byte[] messageVariablePart) {
        byte[] lengthField = new byte[0];
        if (messageVariablePart.length <= (255-2)) {
            byte totalPacketLength = (byte) (1 + 1 + messageVariablePart.length);
            lengthField = new byte[] {totalPacketLength};
        } else if (messageVariablePart.length <= (65535-4)) {
            int totalPacketLength = 3 + 1 + messageVariablePart.length;
            lengthField = new byte[3];
            lengthField[0] = (byte) 0x01;
            lengthField[1] = (byte) ((totalPacketLength & 0xFF00) >>> 8);
            lengthField[2] = (byte) (totalPacketLength & 0x00FF);
        }

        return lengthField;
    }
}
