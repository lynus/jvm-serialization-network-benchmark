package serializers.RDMA;


import com.ibm.darpc.DaRPCServerEndpoint;
import com.ibm.darpc.DaRPCServerEvent;
import com.ibm.darpc.DaRPCService;
import serializers.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;

import static serializers.RDMA.Request.DULL_CMD;
import static serializers.RDMA.Request.TRANS_RDMA;
import static serializers.RDMA.Request.WARMUP_CMD;

public class RPCService extends Protocol implements DaRPCService<Request, Response> {
    private TestGroups groups;
    private Object testData;
    private ArrayBlockingQueue<DataEndpoint> eps;
    private ArrayBlockingQueue<SocketChannel> scs;
    private TestGroup.Entry entry;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(SerTestCase.SIZE);
    public RPCService(TestGroups groups, Object testData, ArrayBlockingQueue eps, ArrayBlockingQueue scs) {
        this.groups = groups;
        this.testData = testData;
        this.eps = eps;
        this.scs = scs;
    }
    protected TestCase Create = new TestCase()
    {
        public <J> double run(Transformer<J,Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception
        {
            for (int i = 0; i < iterations; i++)
            {
                transformer.forward(value);
            }
            return 0;
        }
    };
    protected TestCase deSer = new TestCase()
    {
        public <J> double run(Transformer<J,Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception
        {
            byte[] array = serializer.serialize(transformer.forward(value));
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++)
            {
                serializer.deserialize(array);
            }
            return (double)(System.nanoTime() - start) / iterations;
        }
    };

    public void processServerEvent(DaRPCServerEvent<Request, Response> event) throws IOException {
        Request request = event.getReceiveMessage();
        Response response = event.getSendMessage();
        DataEndpoint ep = eps.peek();
        SocketChannel channel = scs.peek();
        if (ep == null) {
            throw new IOException("no data ep established");
        }
        if (request.cmd == DULL_CMD) {
            event.triggerResponse();
        } else if (request.cmd == WARMUP_CMD) {
            response.cmd = WARMUP_CMD;
            int hash = request.serializer;
            String foundName = null;
            for (String name : groups.media.entries.keySet()) {
                if (name.hashCode() == hash) {
                    foundName = name;
                    break;
                }
            }
            if (foundName == null) {
                response.status = Response.FAILED;
                event.triggerResponse();
                return;
            }
            TestGroup.Entry entry = groups.media.entries.get(foundName);
            System.err.println("warm up deserializer: " + foundName);
            TestCaseRunner runner = new TestCaseRunner(entry.transformer, entry.serializer, testData);
            this.entry = entry;
            try {
                warmTest(runner, request.address, Create);
                warmTest(runner, request.address, deSer);
                ep.registerBuffer(buffer);
            } catch (Exception ex) {
                response.status = Response.FAILED;
                event.triggerResponse();
                return;
            }
            System.err.println("warm up done");
            response.status = Response.SUCCESS;
            event.triggerResponse();
        } else if (request.cmd == TRANS_RDMA) {
            response.cmd = TRANS_RDMA;
            int readBytes = 0;
            buffer.clear();
            while (readBytes < request.bufferSize) {
                readBytes += channel.read(buffer);
            }
	    buffer.clear();
            buffer.putInt(1);
            buffer.flip();
            channel.write(buffer);  //send socket ack
            long start = System.nanoTime();
            ep.postRead(request.address, request.rkey, request.bufferSize);
            try {
                ep.waitEvent();
            } catch (InterruptedException ex) {
            }
            double networkTime = (double) (System.nanoTime() - start) / (double)request.objNumber;
            //Now deserialize
            start = System.nanoTime();
            int i = 0;
            buffer.clear();
            byte[] bytes = null;
            while (i < request.objNumber) {
                int len;
                i++;
                len = buffer.getInt();
                if (bytes == null)
                    bytes = new byte[len];

                buffer.get(bytes, 0, len);
                try {
                    this.entry.serializer.deserialize(bytes);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
            double deserTime = (double)(System.nanoTime() - start) / (double)request.objNumber;
            response.status = Response.SUCCESS;
            response.networkNanos = networkTime;
            response.deSerNanos = deserTime;
            event.triggerResponse();
        }
    }

    @Override
    public void open(DaRPCServerEndpoint<Request, Response> daRPCServerEndpoint) {

    }

    @Override
    public void close(DaRPCServerEndpoint<Request, Response> daRPCServerEndpoint) {

    }

    protected <J> void warmTest(TestCaseRunner<J> runner, long warmupTime, TestCase test) throws Exception
    {
        // Instead of fixed counts, let's try to prime by running for N seconds
        long endTime = System.currentTimeMillis() + warmupTime;
        do {
            runner.run(test, 10);
        }
        while (System.currentTimeMillis() < endTime);
    }
}
