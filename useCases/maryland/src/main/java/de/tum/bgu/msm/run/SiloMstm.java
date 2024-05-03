package de.tum.bgu.msm.run;

import de.tum.bgu.msm.SiloModel;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.container.ModelContainer;
import de.tum.bgu.msm.io.output.DefaultResultsMonitor;
import de.tum.bgu.msm.io.output.ResultsMonitor;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 * Implements SILO for for the Maryland Statewide Transportation Model
 *
 * @author Rolf Moeckel
 */
public class SiloMstm {

    private final static Logger logger = Logger.getLogger(SiloMstm.class);

    public static void main(String[] args) {

        String propertiesPath = System.getenv("PROPERTIES_FILE");
        Properties properties = SiloUtil.siloInitialization(propertiesPath);

        Config config = null;
        String configPath = System.getenv("CONFIG_FILE");
        if (configPath != null && !configPath.isEmpty()) {
            config = ConfigUtils.loadConfig(configPath);
        }
        logger.info("Started SILO land use model for the Maryland Statewide Model Area");
        DataContainer dataContainer = DataBuilder.buildDataContainer(properties, config);
        DataBuilder.readInput(properties, dataContainer);

        ModelContainer modelContainer = ModelBuilderMstm.getModelContainerForMstm(dataContainer, properties, config);

        ResultsMonitor resultsMonitor = new DefaultResultsMonitor(dataContainer, properties);
        SiloModel model = new SiloModel(properties, dataContainer, modelContainer);
        model.addResultMonitor(resultsMonitor);
        ResultsMonitor mstmMonitor = new MstmMonitor(dataContainer, properties);
        model.addResultMonitor(mstmMonitor);
        model.runModel();
        logger.info("Finished SILO.");
    }
}
