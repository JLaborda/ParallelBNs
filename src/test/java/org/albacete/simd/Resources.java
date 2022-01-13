package org.albacete.simd;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.utils.Utils;

public class Resources {
    public static final String ALARM_NET_PATH = "./src/test/res/networks/alarm.xbif";
    public static final String ALARM_BBDD_PATH = "./src/test/res/BBDD/alarm.xbif_.csv";
    public static final String ALARM_TEST_PATH = "./src/test/res/BBDD/tests/alarm_test.csv";
    public static final String CANCER_BBDD_PATH = "./src/test/res/BBDD/cancer.xbif_.csv";
    public static final String CANCER_NET_PATH = "./src/test/res/networks/cancer.xbif";
    public static final DataSet CANCER_DATASET = Utils.readData(CANCER_BBDD_PATH);

    //Variables of Cancer's dataset
    public static final Node XRAY = CANCER_DATASET.getVariable("Xray");
    public static final Node DYSPNOEA = CANCER_DATASET.getVariable("Dyspnoea");
    public static final Node CANCER = CANCER_DATASET.getVariable("Cancer");
    public static final Node POLLUTION = CANCER_DATASET.getVariable("Pollution");
    public static final Node SMOKER = CANCER_DATASET.getVariable("Smoker");

}
