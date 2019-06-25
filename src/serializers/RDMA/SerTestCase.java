package serializers.RDMA;

import com.ibm.darpc.DaRPCFuture;
import com.ibm.disni.verbs.IbvMr;
import serializers.Serializer;
import serializers.TestCase;
import serializers.Transformer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SerTestCase extends TestCase {
    public int lkey, rkey;
    public long address;
    public static final int SIZE = 4 << 20;
    private ByteBuffer buffer;
    private ByteBuffer ackBuffer;

    public void init(RDMAClient client)  {
        buffer = ByteBuffer.allocateDirect(SIZE);
        ackBuffer = ByteBuffer.allocateDirect(16);
        try {
            IbvMr mr = client.registerBuffer(buffer);
            lkey = mr.getLkey();
            rkey = mr.getRkey();
            address = mr.getAddr();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
    @Override
    public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
        buffer.clear();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i+=10) {
            Object input = transformer.forward(value);
            byte[] array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
            input = transformer.forward(value);
            array = serializer.serialize(input);
            buffer.putInt(array.length);
            buffer.put(array);
        }
        if (buffer.position() > SIZE) {
            throw new Exception("byte buffer overflow");
        }
        return (double)(System.nanoTime() - start) / (double)iterations;
    }

    public class NetworkAndDeserTestCase extends TestCase {
        public double networkTime, deserTime, socketTime;
        private RDMAClient client;

        public NetworkAndDeserTestCase(RDMAClient client) {
            this.client = client;
        }
        @Override
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            DaRPCFuture<Request, Response> future = client.sendTransfer(rkey, address, buffer.position(), iterations, serializer.getName());
            long start = System.nanoTime();
            client.socketTransfer(buffer);
	    client.waitAck(ackBuffer);
            socketTime = (double)(System.nanoTime() - start) / iterations;
            while (!future.isDone()) {
            }
            Response resp = future.getReceiveMessage();
            networkTime = resp.getNetworkTime();
            deserTime = resp.getDeSerTime();
            return 0;
        }
    }
}
