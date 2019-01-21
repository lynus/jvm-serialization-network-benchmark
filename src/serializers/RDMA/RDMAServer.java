package serializers.RDMA;

import com.ibm.darpc.DaRPCServerEndpoint;
import com.ibm.darpc.DaRPCServerGroup;
import com.ibm.disni.RdmaActiveEndpointGroup;
import com.ibm.disni.RdmaServerEndpoint;
import serializers.BenchmarkRunner;
import serializers.TestGroup;
import serializers.TestGroups;

import javax.xml.crypto.Data;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;

public class RDMAServer extends BenchmarkRunner {
    public void run(String[] args) throws  Exception {
        String host = args[0];
        TestGroups groups = new TestGroups();
        addTests(groups);
        Params params = new Params();
        findParameters(args, params);

        ArrayBlockingQueue<DataEndpoint> eps = new ArrayBlockingQueue<>(10);
        DataEndpointAcceptor acceptor = new DataEndpointAcceptor(params, eps);
        acceptor.start();

        TestGroup<?> bootstrapGroup = findGroupForTestData(groups, params);
        Object testData = loadTestData(bootstrapGroup, params);

        RPCService rpcService = new RPCService(groups, testData, eps);
        long[] affinity = new long[]{1};

        DaRPCServerGroup<Request, Response> rpcServerGroup = DaRPCServerGroup.createServerGroup(rpcService, affinity,
                10, 0, true, 1, 1, 10, 32);
        RdmaServerEndpoint<DaRPCServerEndpoint<Request, Response>> serverEp = rpcServerGroup.createServerEndpoint();
        InetSocketAddress address = new InetSocketAddress(params.host, 1919);
        serverEp.bind(address, 100);
        while (true) {
            serverEp.accept();
        }

    }
    public static void main(String[] args) {
        try {
            new RDMAServer().run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private class DataEndpointAcceptor extends Thread {
        private Params params;
        private ArrayBlockingQueue<DataEndpoint> eps;

        public DataEndpointAcceptor(Params params, ArrayBlockingQueue<DataEndpoint> eps) {
            this.params = params;
            this.eps = eps;
        }
        @Override
        public void run() {
            try {
                RdmaActiveEndpointGroup<DataEndpoint> endpointGroup = new RdmaActiveEndpointGroup<DataEndpoint>(10,
                        false, 10, 10, 10);
                endpointGroup.init(new DataEndpoint.DataEndpointFactory(endpointGroup));
                RdmaServerEndpoint<DataEndpoint> serverEp = endpointGroup.createServerEndpoint();
                serverEp.bind(new InetSocketAddress(params.host, 2020), 10);
                while (true) {
                    DataEndpoint ep = serverEp.accept();
                    if (eps.size() > 0)
                        if (!eps.take().isClosed())
                            System.err.println("ONLY one data connect a time!");
                    eps.add(ep);
                }
            } catch (Exception exp) {
                exp.printStackTrace();
                System.exit(1);
            }
        }
    }

}
