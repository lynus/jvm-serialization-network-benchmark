package serializers.intruder;

import data.media.MediaContent;
import intruder.*;
import org.jikesrvm.runtime.Magic;
import serializers.BenchmarkRunner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Server extends BenchmarkRunner {
    public static void main(String args[]) {
        Utils.disableLog();
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
        int _loop = loop.intValue();
        Integer iteration = (Integer) inStream.readObject();
        int _iteration = iteration.intValue();
        MediaContent obj = null;
        long start, end;
        long begin = System.nanoTime();
        for  (int i = 0; i < _loop; i++) {
            inStream.spin();
            start = System.nanoTime();
            for (int j = 0; j < _iteration; j++) {
                obj = (MediaContent) inStream.readObject();
            }
            end = System.nanoTime();
//            System.gc();
            inStream.setFinish();
            System.err.println("finish loop #" + i + "avg time: " + (end - start)/_iteration);
        }
        System.out.println("sever average time: " + ((double)(System.nanoTime() - begin))/_loop/_iteration);
        Thread.sleep(1000);
        System.out.println(obj.toString());
        System.exit(0);
    }
}
