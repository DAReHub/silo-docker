package de.tum.bgu.msm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author dziemke, nagel
 */
public class SiloMatsimTest {
	private static final Logger log = Logger.getLogger(SiloMatsimTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * This test does only test the downstream coupling: SILO data is given to MATSim and then iterated.
	 * Possible feedback from MATSim to SILO is NOT included in this test (as it was also not included
	 * in the ABMTRANS'16 paper).
	 */
	@Test
	public final void testMainReduced() {
		SiloMstmTest.cleanUp();

		boolean cleanupAfterTest = false; // Set to true normally; set to false to be able to inspect files
		String arg = "./test/scenarios/annapolis_reduced/javaFiles/siloMatsim_reduced.properties";
		Config config = ConfigUtils.loadConfig("./test/scenarios/annapolis_reduced/matsim_input/config.xml") ;

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.global().setNumberOfThreads(1);
		config.parallelEventHandling().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);

		try {
			SiloMatsim siloMatsim = new SiloMatsim(arg, config);
			siloMatsim.run();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Something did not work") ;
		}
		{
			log.info("Checking dwellings file ...");
			long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "./dd_2001.csv");
			final String filename = "./test/scenarios/annapolis_reduced/microData_reduced/dd_2001.csv";
			long checksum_run = CRCChecksum.getCRCFromFile(filename);
			assertEquals("Dwelling files are different", checksum_ref, checksum_run);
			if (cleanupAfterTest) new File(filename).delete();
		}{
			log.info("Checking households file ...");
			long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "./hh_2001.csv");
			final String filename = "./test/scenarios/annapolis_reduced/microData_reduced/hh_2001.csv";
			long checksum_run = CRCChecksum.getCRCFromFile(filename);
			assertEquals("Household files are different", checksum_ref, checksum_run);
			if (cleanupAfterTest) new File(filename).delete();
		}{
			log.info("Checking jobs file ...");
			long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "./jj_2001.csv");
			final String filename = "./test/scenarios/annapolis_reduced/microData_reduced/jj_2001.csv";
			long checksum_run = CRCChecksum.getCRCFromFile(filename);
			assertEquals("Job files are different", checksum_ref, checksum_run);
			if (cleanupAfterTest) new File(filename).delete();
		}{
			log.info("checking SILO population file ...");
			long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "./pp_2001.csv");
			final String filename = "./test/scenarios/annapolis_reduced/microData_reduced/pp_2001.csv";
			long checksum_run = CRCChecksum.getCRCFromFile(filename);
			assertEquals("Population files are different", checksum_ref, checksum_run);
			if (cleanupAfterTest) new File(filename).delete();
		}{
			log.info("Checking MATSim plans file ...");

			final String referenceFilename = utils.getInputDirectory() + "./test_reduced_2001.0.plans.xml.gz";
			final String outputFilename = utils.getOutputDirectory() + "./test_reduced_matsim_2001/ITERS/it.0/test_reduced_matsim_2001.0.plans.xml.gz";

			Scenario scRef = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
			Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
			
			new PopulationReader(scRef).readFile(referenceFilename);
			new PopulationReader(scOut).readFile(outputFilename);
			
			assertTrue("MATSim populations are different", PopulationUtils.equalPopulation( scRef.getPopulation(), scOut.getPopulation() ) ) ; 
		}{
			log.info("Checking MATSim events file ...");
			final String eventsFilenameReference = utils.getInputDirectory() + "./test_reduced_2001.0.events.xml.gz";
			final String eventsFilenameNew = utils.getOutputDirectory() + "./test_reduced_matsim_2001/ITERS/it.0/test_reduced_matsim_2001.0.events.xml.gz";
			assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
		}
		
		// TODO Consider checking accessibilities (currently stored in "testing" directory)
		
		if (cleanupAfterTest) {
			File dir = new File(utils.getOutputDirectory());
			IOUtils.deleteDirectory(dir);
			SiloMstmTest.cleanUp();
		}
	}
}