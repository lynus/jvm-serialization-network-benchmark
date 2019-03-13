package serializers.RDMA;

import com.ibm.darpc.*;
import serializers.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class RDMAItemBenchmark extends BenchmarkRunner {
    public static void main(String[] args) {
        new RDMAItemBenchmark().runBenchmark(args);
    }

    protected RDMAClient client = new RDMAClient();

    private String findAddress(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-host")) {
                int eqPos = arg.indexOf('=');
                return arg.substring(eqPos + 1);
            }
        }
        return null;
    }
    @Override
    protected void runBenchmark(String[] args) {
        try {
            client.connect(findAddress(args));
            mySerialize.init(client);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        runBenchmark(args, Create, mySerialize, networkDeser);
        try {
            runRPCDelayTest(args);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        client.close();
    }

    protected SerTestCase mySerialize = new SerTestCase();
    protected SerTestCase.NetworkAndDeserTestCase networkDeser = mySerialize.new NetworkAndDeserTestCase(client);

    protected static void addValue(
            EnumMap<measurements, Map<String, Double>> values,
            String name,
            double timeNetwork) {
        values.get(measurements.timeNetwork).put(name, timeNetwork);
    }

    @Override
    protected <J> EnumMap<measurements, Map<String, Double>> runMeasurements
            (PrintWriter errors,
             Params params, Iterable<TestGroup.Entry<J, Object>> groups, J value,
             TestCase testCreate, TestCase testSerialize, TestCase testDeserialize) throws Exception {
        // Check correctness first.
        System.out.println("Checking correctness...");
        for (TestGroup.Entry<J,Object> entry : groups)
        {
            checkCorrectness(errors, entry.transformer, entry.serializer, value);
        }
        System.out.println("[done]");

        if (params.prewarm) {
            System.out.print("Pre-warmup...");
            for (TestGroup.Entry<J,Object> entry : groups)
            {
                TestCaseRunner<J> runner = new TestCaseRunner<J>(entry.transformer, entry.serializer, value);
                String name = entry.serializer.getName();
                System.out.print(" " + name);

                DaRPCFuture<Request, Response> future = client.sendWarmup(entry.serializer.getName(), params.warmupTime);

                warmTest(runner, params.warmupTime, testCreate);
                warmTest(runner, params.warmupTime, testSerialize);

                while (!future.isDone()) { }
                if (future.getReceiveMessage().status == Response.FAILED)
                    throw new Exception("remote warm up failed");
            }
            System.out.println();
            System.out.println("[done]");
        }

        System.out.printf("%-34s %6s %7s %7s %7s %7s %6s %5s %7s\n",
                params.printChart ? "\npre." : "",
                "create",
                "ser",
                "deser",
                "network",
                "total",
                "size",
                "+dfl",
                "socket");
        EnumMap<measurements, Map<String, Double>> values = new EnumMap<measurements, Map<String, Double>>(measurements.class);
        for (measurements m : measurements.values())
            values.put(m, new HashMap<String, Double>());

        // Actual tests.
        for (TestGroup.Entry<J,Object> entry : groups)
        {
            TestCaseRunner<J> runner = new TestCaseRunner<J>(entry.transformer, entry.serializer, value);
            String name = entry.serializer.getName();
            try {
                warmTest(runner, params.warmupTime / 3, testCreate);
                doGc();
                double timeCreate = runner.runWithTimeMeasurement(params.testRunMillis / 3, testCreate, params.iterations);

                double timeSerialize = runner.runWithTimeMeasurement(params.testRunMillis, testSerialize, params.iterations);
                double[]  times = runner.runWithTimeMeasurement_2results(params.testRunMillis,
                        testDeserialize, params.iterations);
                double timeNetwok = times[0];
                double timeDeserialize = times[1];
                double timeSocket = times[2];
                double totalTime = timeSerialize + timeDeserialize + timeNetwok;
                byte[] array = serializeForSize(entry.transformer, entry.serializer, value);
                byte[] compressDeflate = compressDeflate(array);

                System.out.printf("%-34s %6.0f %7.0f %7.0f %7.0f %7.0f %6d %5d %7.0f\n",
                        name,
                        timeCreate,
                        timeSerialize,
                        timeDeserialize,
                        timeNetwok,
                        totalTime,
                        array.length,
                        compressDeflate.length,
                        timeSocket
                        );


                addValue(values, name, timeCreate, timeSerialize,
                        timeDeserialize, totalTime,
                        array.length, compressDeflate.length);
                addValue(values, name, timeNetwok);
            } catch (Exception ex) {
                System.out.println("ERROR: \"" + name + "\" crashed during benchmarking.");
                errors.println(ERROR_DIVIDER);
                errors.println("\"" + name + "\" crashed during benchmarking.");
                ex.printStackTrace(errors);
            }
        }
        return values;
    }

    private void runRPCDelayTest(String[] args) throws IOException {
        System.err.println("test dull rpc round delay");
        Params params = new Params();
        findParameters(args, params);
        long start = System.nanoTime();
        for (int i = 0; i < params.iterations; i++) {
             DaRPCFuture future = client.sendDull();
             while (!future.isDone()) {}
        }
        long end = System.nanoTime();
        DecimalFormat format = new DecimalFormat("#.000");
        System.err.println(format.format((end - start) / params.iterations)+ " ns");
    }

}
