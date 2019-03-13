import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MakeGraph {
    public static class Record{
        String name;
        long serTime;
        long deserTime;
        long transTime;
	long socketTime;

        public Record(String name, long serTime, long deserTime, long transTime, long socketTime) {
            this.name = name;
            this.serTime = serTime;
            this.deserTime = deserTime;
            this.transTime = transTime;
            this.socketTime = socketTime;
        }
        public long totalTime() {
            return serTime + deserTime + transTime;
        }

        public long socketTotalTime() {
            return serTime + deserTime + socketTime;
        }
        public String transRate() {
            return String.valueOf(transTime * 100 / socketTotalTime()) + '%';
        }
        public String socketRate() {
            return String.valueOf(socketTime * 100 / totalTime()) + '%';
        }
    }

    public static class SortByTrans implements Comparator<Record> {
        public int compare(Record A, Record B) {
            return (int)(A.totalTime() - B.totalTime());
        }
    }
    public static void main(String[] args) {
        Reader reader = null;
        ArrayList<Record> list = new ArrayList<Record>();
        try {
            reader = new FileReader(args[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        StreamTokenizer tk = new StreamTokenizer(reader);
        tk.eolIsSignificant(true);
        tk.wordChars('+', '/');
        try {
            while (tk.nextToken() != StreamTokenizer.TT_EOL) {}
            while (tk.nextToken() != StreamTokenizer.TT_EOF) {
                String serializer = tk.sval;

                tk.nextToken();
                tk.nextToken();
                long serTime = (long)tk.nval;
                tk.nextToken();
                long deserTime = (long)tk.nval;
                tk.nextToken();
                long transTime = (long)tk.nval;
                tk.nextToken();
                tk.nextToken();
                tk.nextToken();
                tk.nextToken();
		long socketTime = (long)tk.nval;
                while(tk.nextToken() != StreamTokenizer.TT_EOL) {}
                list.add(new Record(serializer, serTime, deserTime, transTime, socketTime));
            }
            Collections.sort(list, new SortByTrans());
            outputChart(list, Integer.parseInt(args[1]));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
/* Sample google chart API usage
   <head>
  <script src="https://www.gstatic.com/charts/loader.js">
  </script>
  <script type="text/javascript">
    google.charts.load('current', {packages: ['corechart']});
    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {
        var data = new google.visualization.arrayToDataTable([
        ['serializer', 'ser', 'deser', 'network'],
        ['avro-generic', 2646, 1479, 89],
        ['avro-specific', 2578,1790, 91],
        ['capnproto', 3304, 1547,104],
        ['fst-flat', 1966, 1853, 98]
        ]
      );
      var options = {
        width: 600,
        height: 400,
        legend: { position: 'top', maxLines: 3 },
        bar: { groupWidth: '75%' },
        isStacked: true
      };

      var chart = new google.visualization.BarChart(document.getElementById("chart"));
    chart.draw(data, options);
    }
    </script>
    </head>
    <body>
      <div id='chart' />
    </body>
 */
    public static void outputChart(ArrayList<Record> list, int n) throws IOException {
        FileWriter writer = new FileWriter("chart.html");
        String preamble = " <head>\n" +
                "  <script src=\"https://www.gstatic.com/charts/loader.js\">\n" +
                "  </script>\n" +
                "  <script type=\"text/javascript\">\n" +
                "    google.charts.load('current', {packages: ['corechart']});\n" +
                "    google.charts.setOnLoadCallback(drawChart);\n" +
                "\n" +
                "    function drawChart() {\n" +
                "        var data = new google.visualization.arrayToDataTable([\n" +
                "['serializer', 'ser', 'deser', 'network', 'socket', {role: 'annotation'}],\n";
        writer.write(preamble);
        for (int i = 0; i < n; i++) {
            String row = "['" + list.get(i).name + "'," + list.get(i).serTime + ", " +
                    list.get(i).deserTime + ", " + list.get(i).transTime + ", " + list.get(i).socketTime+", '"+
		    "r: "+list.get(i).transRate() + " s: "+ list.get(i).socketRate() + "'],\n";
            writer.write(row);
        }
        String height = "height: " + 30 * n + ",\n";
        String postamble = " ]\n" +
                "      );\n" +
                "      var options = {\n" +
                "        width: 600,\n" +
                height +
                "        legend: { position: 'top', maxLines: 3 },\n" +
                "        bar: { groupWidth: '75%' },\n" +
                "        isStacked: true,\n" +
                "        vAxis:{textStyle:{fontSize:10}}\n" +
                "      };\n" +
                "\n" +
                "      var chart = new google.visualization.BarChart(document.getElementById(\"chart\"));\n" +
                "    chart.draw(data, options);\n" +
                "    }\n" +
                "    </script>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "      <div id='chart' />\n" +
                "    </body>";
        writer.write(postamble);
        writer.close();
    }
}
