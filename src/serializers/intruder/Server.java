package serializers.intruder;

import data.media.MediaContent;
import intruder.*;
import serializers.BenchmarkRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Server extends BenchmarkRunner {
    public static void main(String args[]) {
        RegisterRdmaClass.registerType();
        try {
            new Server().run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    public void run(String args[]) throws Exception {
        Params params = new Params();
        findParameters(args, params);
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(params.host), 8090);
        Listener listener = Factory.newListener(address);
        Endpoint ep = listener.accept();
        IntruderInStream inStream = ep.getInStream();
        System.err.println("get here! stream id: " + inStream.getConnectionId());
        Integer loop = (Integer) inStream.readObject();
        System.err.println("get loop: " + loop);
        Integer iteration = (Integer) inStream.readObject();
        MediaContent obj = null;
        long total = 0L;
        for  (int i = 0; i < loop; i++) {
            long start = System.nanoTime();
            System.err.println("loop: " + i);
            for (int j = 0; j < iteration; j++) {
                obj = (MediaContent) inStream.readObject();
            }
            total += System.nanoTime() - start;
            //inStream.setFinish();
        }
        System.out.println("sever average time: " + (double)total/loop/iteration);
    }
}
