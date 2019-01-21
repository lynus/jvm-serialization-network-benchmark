package serializers.RDMA;

import com.ibm.darpc.*;
import com.ibm.disni.RdmaActiveEndpointGroup;
import com.ibm.disni.verbs.IbvMr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class RDMAClient {
    private static DaRPCClientGroup<Request, Response> rpcClientGroup;
    private DaRPCClientEndpoint<Request, Response> rpcEp;
    private DaRPCStream<Request, Response> stream;
    private Request request;
    private Response response;
    private static RdmaActiveEndpointGroup<DataEndpoint> dataGroup;
    private DataEndpoint dataEp;
    static {
        try {
            rpcClientGroup = DaRPCClientGroup.createClientGroup(new Protocol(), 10, 0, 1, 1);
            dataGroup = new RdmaActiveEndpointGroup<DataEndpoint>(10,false, 10, 16, 10);
            dataGroup.init(new DataEndpoint.DataEndpointFactory(dataGroup));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void connect(String host) throws Exception {
        rpcEp = rpcClientGroup.createEndpoint();
        InetSocketAddress address = new InetSocketAddress(host, 1919);
        while (true) {
            try {
                rpcEp.connect(address, 10);
                break;
            } catch (IOException ex) {
               System.err.println("connect error: " + ex.getMessage() + " retry after 10 secs");
               ex.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException _ex) {
                }
//                rpcEp.close();
//                rpcEp = rpcClientGroup.createEndpoint();
            }
        }

        stream = rpcEp.createStream();
        request = new Request();
        response = new Response();

        dataEp = dataGroup.createEndpoint();
        address = new InetSocketAddress(host, 2020);
        while (true) {
            try {
                dataEp.connect(address, 10);
                break;
            } catch (IOException ex) {
                System.err.println("connect error: " + ex.getMessage() + " retry after 10 secs");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException _ex) {
                }
//                dataEp.close();
//                dataEp = dataGroup.createEndpoint();
            }
        }
    }

    public void close() {
        try {
            rpcEp.close();
            rpcClientGroup.close();
            dataEp.close();
            dataGroup.close();
        } catch (Exception ex) {
        }
    }

    public IbvMr registerBuffer(ByteBuffer buffer) throws IOException {
        return dataEp.registerMemory(buffer).execute().free().getMr();
    }

    public DaRPCFuture<Request, Response> sendWarmup(String serName, long warmTime) {
        request.warmup(serName, warmTime);
        try {
            return stream.request(request, response, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public DaRPCFuture<Request, Response> sendTransfer(int rkey, long addr, int size, int iterations, String serName)
    throws Exception {
        request.mkTRANS_RDMA(serName, addr, rkey, size, iterations);
        return stream.request(request, response, false);
    }
}
