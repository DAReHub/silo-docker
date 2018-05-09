/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package de.tum.bgu.msm;

import cern.colt.Timer;
import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.data.HouseholdDataManager;
import de.tum.bgu.msm.data.SummarizeData;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.events.EventManager;
import de.tum.bgu.msm.events.EventType;
import de.tum.bgu.msm.events.IssueCounter;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SkimUtil;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Greg Erhardt
 * Created on Dec 2, 2009
 */
public final class SiloModel {
	private final static Logger LOGGER = Logger.getLogger(SiloModel.class);

	private final Set<Integer> tdmYears = new HashSet<>();
	private final Set<Integer> skimYears = new HashSet<>();
    private final Set<Integer> scalingYears = new HashSet<>();

    private boolean trackTime;
    private long[][] timeCounter;

    private Timer timer = new Timer();

	private SiloModelContainer modelContainer;
	private SiloDataContainer dataContainer;
	private final Config matsimConfig;
    private EventManager em;

    public SiloModel() {
		this(null) ;
	}

	public SiloModel(Config matsimConfig) {
		IssueCounter.setUpCounter();
		SiloUtil.modelStopper("initialize");
		this.matsimConfig = matsimConfig ;
	}

	public void runModel() {
		if (!Properties.get().main.runSilo) {
			return;
		}
		setupModel();
		runYearByYear();
		endSimulation();
	}

	private void setupModel() {
		LOGGER.info("Setting up SILO Model (Implementation " + Properties.get().main.implementation + ")");

        setupContainer();
        setupYears();
        setupAccessibility();
        setupTimeTracker();
        setupEventManager();
        IssueCounter.logIssues(dataContainer.getGeoData());

        if (Properties.get().main.writeSmallSynpop) {
            dataContainer.getHouseholdData().writeOutSmallSynPop();
        }
		if (Properties.get().main.createPrestoSummary) {
			SummarizeData.preparePrestoSummary(dataContainer.getGeoData());
		}
	}

    private void setupContainer() {
        dataContainer = SiloDataContainer.loadSiloDataContainer(Properties.get());
		IssueCounter.regionSpecificCounters(dataContainer.getGeoData());
		dataContainer.getHouseholdData().setTypeOfAllHouseholds();
		dataContainer.getHouseholdData().setHighestHouseholdAndPersonId();
		dataContainer.getHouseholdData().calculateInitialSettings();
		dataContainer.getJobData().calculateEmploymentForecast();
		dataContainer.getJobData().identifyVacantJobs();
		dataContainer.getJobData().calculateJobDensityByZone();
		dataContainer.getRealEstateData().fillQualityDistribution();
		dataContainer.getRealEstateData().setHighestVariables();
		dataContainer.getRealEstateData().identifyVacantDwellings();

        modelContainer = SiloModelContainer.createSiloModelContainer(dataContainer, matsimConfig);
    }

    private void setupAccessibility() {

	    if(Properties.get().transportModel.runMatsim) {
	        modelContainer.getTransportModel().runTransportModel(Properties.get().main.startYear);
        } else {
            SkimUtil.updateCarSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                    Properties.get().main.startYear, Properties.get());
			SkimUtil.updateTransitSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
					Properties.get().main.startYear, Properties.get());
			// updateTransitSkim was outside the bracket before, but central code should not decide how matsim updates
			// travel times.  If necessary, should be done inside the matsim adapter class. kai, may'18
        }
        modelContainer.getAcc().initialize();
        modelContainer.getAcc().calculateHansenAccessibilities(Properties.get().main.startYear);
    }

    private void setupTimeTracker() {
		trackTime = Properties.get().main.trackTime;
		timeCounter = new long[EventType.values().length + 12][Properties.get().main.endYear + 1];
		timer = new Timer();
	}

	private void setupYears() {
		scalingYears.addAll(Properties.get().main.scalingYears);
		if (!scalingYears.isEmpty()) {
			SummarizeData.readScalingYearControlTotals();
		}
		tdmYears.addAll(Properties.get().transportModel.modelYears);
		skimYears.addAll(Properties.get().transportModel.skimYears);
	}

	private void setupEventManager() {
        em = new EventManager(timeCounter);

		if(Properties.get().eventRules.allDemography) {
			if (Properties.get().eventRules.birthday || Properties.get().eventRules.birth) {
                em.registerEventHandler(modelContainer.getBirth());
                em.registerEventCreator(modelContainer.getBirth());
            }
            if(Properties.get().eventRules.death) {
                em.registerEventHandler(modelContainer.getDeath());
                em.registerEventCreator(modelContainer.getDeath());
            }
            if(Properties.get().eventRules.leaveParentHh) {
                em.registerEventHandler(modelContainer.getLph());
                em.registerEventCreator(modelContainer.getLph());
            }
            if(Properties.get().eventRules.divorce ||Properties.get().eventRules.marriage) {
			    em.registerEventHandler(modelContainer.getMardiv());
                em.registerEventCreator(modelContainer.getMardiv());
            }
            if(Properties.get().eventRules.schoolUniversity) {
                em.registerEventCreator(modelContainer.getChangeSchoolUniv());
                em.registerEventHandler(modelContainer.getChangeSchoolUniv());
            }
            if(Properties.get().eventRules.driversLicense) {
                em.registerEventHandler(modelContainer.getDriversLicense());
                em.registerEventCreator(modelContainer.getDriversLicense());
            }
            if (Properties.get().eventRules.quitJob || Properties.get().eventRules.startNewJob ) {
                em.registerEventHandler(modelContainer.getEmployment());
                em.registerEventCreator(modelContainer.getEmployment());
            }
            if(Properties.get().eventRules.allHhMoves) {
                em.registerEventHandler(modelContainer.getMove());
                em.registerEventCreator(modelContainer.getMove());
                if(Properties.get().eventRules.outMigration) {
                    em.registerEventHandler(modelContainer.getIomig());
                    em.registerEventCreator(modelContainer.getIomig());
                }
            }
            if(Properties.get().eventRules.allDwellingDevelopments) {
                if(Properties.get().eventRules.dwellingChangeQuality) {
                    em.registerEventHandler(modelContainer.getRenov());
                    em.registerEventCreator(modelContainer.getRenov());
                }
                if(Properties.get().eventRules.dwellingDemolition) {
                    em.registerEventHandler(modelContainer.getDemol());
                    em.registerEventCreator(modelContainer.getDemol());
                }
            }
            if(Properties.get().eventRules.dwellingConstruction) {
                em.registerEventHandler(modelContainer.getCons());
                em.registerEventCreator(modelContainer.getCons());
            }
        }
    }

	private void runYearByYear() {
        for (int year = Properties.get().main.startYear; year < Properties.get().main.endYear; year ++) {
            if (scalingYears.contains(year)) {
                SummarizeData.scaleMicroDataToExogenousForecast(year, dataContainer);
            }
            LOGGER.info("Simulating changes from year " + year + " to year " + (year + 1));
            IssueCounter.setUpCounter();    // setup issue counter for this simulation period
            SiloUtil.trackingFile("Simulating changes from year " + year + " to year " + (year + 1));
			final HouseholdDataManager householdData = dataContainer.getHouseholdData();

			if (trackTime) timer.reset();
			if (year != Properties.get().main.implementation.BASE_YEAR) {
				modelContainer.getUpdateJobs().updateJobInventoryMultiThreadedThisYear(year);
				dataContainer.getJobData().identifyVacantJobs();
			}
			if (trackTime) timeCounter[EventType.values().length + 2][year] += timer.millis();

			if (trackTime) timer.reset();

			if (trackTime) timeCounter[EventType.values().length + 4][year] += timer.millis();

			if (skimYears.contains(year) && !tdmYears.contains(year) &&
					!Properties.get().transportModel.runTravelDemandModel &&
					year != Properties.get().main.startYear) {
				// skims are (in non-Matsim case) always read in start year and in every year the transportation model ran. Additional
				// years to read skims may be provided in skimYears
				if (!Properties.get().transportModel.runMatsim) {
                    SkimUtil.updateCarSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                            Properties.get().main.startYear, Properties.get());
				}
                SkimUtil.updateTransitSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                        Properties.get().main.startYear, Properties.get());
                modelContainer.getAcc().calculateHansenAccessibilities(year);
            }

			if (trackTime) timer.reset();
			modelContainer.getDdOverwrite().addDwellings(year);
			if (trackTime) timeCounter[EventType.values().length + 10][year] += timer.millis();

			if (trackTime) timer.reset();
			modelContainer.getMove().calculateRegionalUtilities();
			modelContainer.getMove().calculateAverageHousingSatisfaction();
			if (trackTime) timeCounter[EventType.values().length + 6][year] += timer.millis();

			if (trackTime) timer.reset();
			if (year != Properties.get().main.implementation.BASE_YEAR) householdData.adjustIncome();
			if (trackTime) timeCounter[EventType.values().length + 9][year] += timer.millis();

			if (trackTime) timer.reset();
			if (year == Properties.get().main.implementation.BASE_YEAR || year != Properties.get().main.startYear)
				SiloUtil.summarizeMicroData(year, modelContainer, dataContainer);
			if (trackTime) timeCounter[EventType.values().length + 7][year] += timer.millis();

            em.simulateEvents(year);

			if (trackTime) timer.reset();
			int[] carChangeCounter = modelContainer.getCarOwnershipModel().updateCarOwnership(householdData.getUpdatedHouseholds());
			householdData.clearUpdatedHouseholds();
			if (trackTime) timeCounter[EventType.values().length + 11][year] += timer.millis();

			if ( Properties.get().transportModel.runMatsim || Properties.get().transportModel.runTravelDemandModel
                    || Properties.get().main.createMstmOutput) {
                if (tdmYears.contains(year + 1)) {
                    modelContainer.getTransportModel().runTransportModel(year + 1);
                    modelContainer.getAcc().calculateHansenAccessibilities(year + 1);
                }
            }


			if (trackTime) timer.reset();
			modelContainer.getPrm().updatedRealEstatePrices();
			if (trackTime) timeCounter[EventType.values().length + 8][year] += timer.millis();

			EventManager.logEvents(carChangeCounter, dataContainer);
			IssueCounter.logIssues(dataContainer.getGeoData());           // log any issues that arose during this simulation period

			LOGGER.info("  Finished this simulation period with " + householdData.getPersonCount() +
					" persons, " + householdData.getHouseholds().size() + " households and "  +
					dataContainer.getRealEstateData().getDwellings().size() + " dwellings.");
			if (SiloUtil.modelStopper("check")) break;
		}
	}

	private void endSimulation() {
		if (scalingYears.contains(Properties.get().main.endYear)) {
            SummarizeData.scaleMicroDataToExogenousForecast(Properties.get().main.endYear, dataContainer);
        }

		dataContainer.getHouseholdData().summarizeHouseholdsNearMetroStations(modelContainer);

		if (Properties.get().main.endYear != 2040) {
			SummarizeData.writeOutSyntheticPopulation(Properties.get().main.endYear, dataContainer);
			dataContainer.getGeoData().writeOutDevelopmentCapacityFile(dataContainer);
		}

		SiloUtil.summarizeMicroData(Properties.get().main.endYear, modelContainer, dataContainer);
		SiloUtil.finish(modelContainer);
		SiloUtil.modelStopper("removeFile");
		if (trackTime) {
		    SiloUtil.writeOutTimeTracker(timeCounter);
        }
		LOGGER.info("Scenario results can be found in the directory scenOutput/" + Properties.get().main.scenarioName + ".");
	}

	//*************************************************************************
	//*** Special implementation of same SILO Model for integration with    ***
	//*** CBLCM Framework (http://csdms.colorado.edu/wiki/Main_Page). Used  ***
	//*** in Maryland implementation only. Functions:                       ***
	//*** initialize(), runYear (double dt) and finishModel()               ***
	//*************************************************************************

	@Deprecated // use SiloModelCBLCM directly
	private SiloModelCBLCM cblcm ;
	@Deprecated // use SiloModelCBLCM directly
	public void initialize() {
		cblcm = new SiloModelCBLCM() ;
		cblcm.initialize();
	}
	@Deprecated // use SiloModelCBLCM directly
	public void runYear (double dt) {
		cblcm.runYear(dt);
	}
	@Deprecated // use SiloModelCBLCM directly
	public void finishModel() {
		cblcm.finishModel();
	}
}
