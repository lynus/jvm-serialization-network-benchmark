package serializers.RDMA;

import com.ibm.darpc.DaRPCMessage;

import java.nio.ByteBuffer;

import static serializers.RDMA.Request.TRANS_RDMA;

public class Response implements DaRPCMessage {
    public static final int FAILED = -1;
    public static final int SUCCESS = 1;
    public int cmd;
    public int status;
    public double networkNanos;
    public double deSerNanos;

    private static int SIZE = 24;
    public int size() {
        return SIZE;
    }

    @Override
    public void update(ByteBuffer buffer) {
        cmd = buffer.getInt();
        status = buffer.getInt();
        if (cmd == TRANS_RDMA) {
            networkNanos = buffer.getDouble();
            deSerNanos = buffer.getDouble();
        }
    }

    @Override
    public int write(ByteBuffer buffer) {
        buffer.putInt(cmd);
        buffer.putInt(status);
        if (cmd == TRANS_RDMA) {
            buffer.putDouble(networkNanos);
            buffer.putDouble(deSerNanos);
        }
        return SIZE;
    }

    public double getDeSerTime() {
        return deSerNanos;
    }
    public double getNetworkTime() {
        return networkNanos;
    }
}
