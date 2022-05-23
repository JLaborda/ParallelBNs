package consensusBN.circularFusion;

import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.utils.Utils;

public class CircularFusionMain {

    public static void main(String[] args) {
        String alarmDatabasePath = "./res/networks/BBDD/alarm.xbif_.csv";
        Utils.setSeed(42);
        CircularFusion fusion = new CircularFusion(alarmDatabasePath, 2, new RandomClustering());
        Dag result = fusion.union();
        System.out.println(result);
    }
}
