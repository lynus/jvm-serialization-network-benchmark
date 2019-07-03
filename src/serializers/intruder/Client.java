package serializers.intruder;

import intruder.Endpoint;
import intruder.Factory;
import intruder.IntruderOutStream;
import intruder.Utils;
import serializers.BenchmarkRunner;
import serializers.TestGroup;
import serializers.TestGroups;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Client extends BenchmarkRunner {
    public static void main(String args[]) {
        Utils.disableLog();
        RegisterRdmaClass.registerType();
        try {
            new Client().run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void run(String args[]) throws Exception {
        Params params = new Params();
        findParameters(args, params);
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(params.host), 8090);
        Endpoint ep = Factory.newEndpoint();
        ep.connect(address, 10);
        System.err.println("connected!");
        Thread.sleep(1000);
        IntruderOutStream outStream = ep.getOutStream();
        outStream.disableHandle();

        TestGroups groups = new TestGroups();
        TestGroup<?> group = findGroupForTestData(groups, params);
        Object testData = loadTestData(group, params);
        long total = 0L;
        Integer loop = params.loop;
        outStream.writeObject(loop);
        outStream.writeObject(new Integer(params.iterations));
        outStream.flush();
        for (int i = 0; i < loop; i++) {
            long start = System.nanoTime();
            for (int j = 0; j < params.iterations; j++)
                outStream.writeObject(testData);
//            outStream.flush();
            //outStream.waitRemoteFinish();
            total += System.nanoTime() - start;
            System.err.println("finishi loop #" + i);
        }
        outStream.flush();
        System.out.println("average time: " + (double)total/loop/params.iterations);
    }
}