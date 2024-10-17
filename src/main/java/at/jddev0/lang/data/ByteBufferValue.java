package at.jddev0.lang.data;

public final class ByteBufferValue extends DataValue {
    private final byte[] byteBuf;

    public ByteBufferValue(byte[] byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public byte[] getByteBuffer() {
        return byteBuf;
    }
}
