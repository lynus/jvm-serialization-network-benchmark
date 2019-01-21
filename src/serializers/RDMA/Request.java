package serializers.RDMA;

import com.ibm.darpc.DaRPCMessage;

import java.nio.ByteBuffer;

public class Request implements DaRPCMessage {
    static final int WARMUP_CMD = 1;
    static final int TRANS_RDMA= 2;
    private static final int SIZE = 28;
    public int cmd;
    public int serializer;
    public long address;
    public int rkey;
    public int bufferSize;
    public int objNumber;

    public int size() {
        return SIZE;
    }

    public void warmup(String serializer, long warmTime) {
        cmd = WARMUP_CMD;
        this.serializer = serializer.hashCode();
        this.address = warmTime;
    }

    public void mkTRANS_RDMA(String seriaName, long address, int rkey, int size, int objNumber) {
        this.cmd = TRANS_RDMA;
        this.serializer = seriaName.hashCode();
        this.address = address;
        this.rkey = rkey;
        this.bufferSize = size;
        this.objNumber = objNumber;
    }

    @Override
    public void update(ByteBuffer buffer) {
        cmd = buffer.getInt();
        serializer = buffer.getInt();
        address = buffer.getLong();
        if (cmd == TRANS_RDMA) {
            rkey = buffer.getInt();
            bufferSize = buffer.getInt();
            objNumber = buffer.getInt();
        }
    }

    @Override
    public int write(ByteBuffer buffer) {
        buffer.putInt(cmd);
        buffer.putInt(serializer);
        buffer.putLong(address);
        if (cmd == TRANS_RDMA) {
            buffer.putInt(rkey);
            buffer.putInt(bufferSize);
            buffer.putInt(objNumber);
        }
        return SIZE;
    }
}
