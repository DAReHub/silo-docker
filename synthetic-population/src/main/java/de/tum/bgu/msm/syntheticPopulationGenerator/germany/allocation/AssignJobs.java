package de.tum.bgu.msm.syntheticPopulationGenerator.germany.allocation;

import com.google.common.math.LongMath;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.RowVector;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.job.Job;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonMuc;
import de.tum.bgu.msm.syntheticPopulationGenerator.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

public class AssignJobs {

    private static final Logger logger = Logger.getLogger(AssignJobs.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;
    private Matrix travelTimeImpedance;

    private HashMap<String, Integer> jobIntTypes;
    protected HashMap<Integer, int[]> idVacantJobsByZoneType;
    protected HashMap<Integer, Integer> numberVacantJobsByType;
    protected HashMap<Integer, int[]> idZonesVacantJobsByType;
    protected HashMap<Integer, Integer> numberVacantJobsByZoneByType;
    protected HashMap<Integer, Integer> numberZonesByType;


    private String[] jobStringTypes;
    private ArrayList<Person> workerArrayList;
    private int assignedJobs;
    private int[] tazIds;


    public AssignJobs(DataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
    }


    public void run() {
        logger.info("   Running module: job de.tum.bgu.msm.syntheticPopulationGenerator.germany.disability");
        calculateTravelTimeImpedance();
        identifyVacantJobsByZoneType();
        shuffleWorkers();
        logger.info("Number of workers " + workerArrayList.size());
        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
        HouseholdDataManager households = dataContainer.getHouseholdDataManager();
        TableDataSet cellsMatrix = PropertiesSynPop.get().main.cellsMatrix;
        for (Person pp : workerArrayList){
            String selectedJobTypeAsString = (String) ((PersonMuc) pp).getAdditionalAttributes().get("jobType");
            ///todo found some empty job types
            if (selectedJobTypeAsString.equals("")){
                ((PersonMuc) pp).getAdditionalAttributes().put("jobType", "Serv");
                selectedJobTypeAsString="Serv";
            }

            int selectedJobType = jobIntTypes.get(selectedJobTypeAsString);

            Household hh = pp.getHousehold();

            //int ddZone = realEstate.getDwelling(hh.getDwellingId()).getZoneId();

            //int origin=0;
            int origin = realEstate.getDwelling(hh.getDwellingId()).getZoneId();

            //for(int cells : cellsMatrix.getColumnAsInt("ID_cell")) {
            //    if ( cells == ddZone) {
            //        origin = (int) cellsMatrix.getIndexedValueAt(cells, "TAZ");
            //        break;
            //    }
            //}

            int[] workplace = selectWorkplace(origin, selectedJobType);

            if (workplace[0] > 0) {
                int jobID = idVacantJobsByZoneType.get(workplace[0])[numberVacantJobsByZoneByType.get(workplace[0]) - 1];
                setWorkerAndJob(pp, jobID);
                updateMaps(selectedJobType, workplace);
            }
            if (LongMath.isPowerOfTwo(assignedJobs)){
                logger.info("   Assigned " + assignedJobs + " jobs.");
            }
            assignedJobs++;
        }
        logger.info("   Finished job de.tum.bgu.msm.syntheticPopulationGenerator.germany.disability. Assigned " + assignedJobs + " jobs.");
    }



   private void calculateTravelTimeImpedance(){

        travelTimeImpedance = new Matrix(dataSetSynPop.getTravelTimeTazToTaz().getRowCount(), dataSetSynPop.getTravelTimeTazToTaz().getColumnCount());
        Map<Integer, Float> utilityHBW = dataSetSynPop.getTripLengthDistribution().column("HBW");
        for (int i = 1; i <= dataSetSynPop.getTravelTimeTazToTaz().getRowCount(); i ++){
            for (int j = 1; j <= dataSetSynPop.getTravelTimeTazToTaz().getColumnCount(); j++){
                int travelTime = (int) dataSetSynPop.getTravelTimeTazToTaz().getValueAt(i,j);
                float utility = 0.00000001f;
                travelTimeImpedance.setValueAt(i, j, travelTime);
                //if (travelTime < 140){
                //    utility = utilityHBW.get(travelTime);
                //}
                if (i==j && travelTime >= 1_000_000_000){
                    //utility = utilityHBW.get(5);
                    travelTimeImpedance.setValueAt(i, j, 5);
                }
                //travelTimeImpedance.setValueAt(i, j, utility);
            }
        }
    }


    private void setWorkerAndJob(Person pp, int jobID){

        dataContainer.getJobDataManager().getJobFromId(jobID).setWorkerID(pp.getId());
        int jobTAZ = dataContainer.getJobDataManager().getJobFromId(jobID).getZoneId();
        pp.setWorkplace(jobID);
    }


    private int[] selectWorkplace(int homeTaz, int selectedJobType){

        int[] workplace = new int[2];
        if (numberZonesByType.get(selectedJobType) > 0) {
            double[] probs = new double[numberZonesByType.get(selectedJobType)];
            double alpha = 0.5000;   //0.50, 0.40, 0.30, 0.25, 0.15
            double gamma =-0.0090;  //-0.0150
            int[] ids = idZonesVacantJobsByType.get(selectedJobType);
            RowVector traveltimes = travelTimeImpedance.getRow(homeTaz);
            IntStream.range(0, probs.length).parallel().forEach(id -> probs[id] = Math.exp(Math.exp(traveltimes.getValueAt((ids[id]-selectedJobType)/100 )*gamma) * Math.pow(numberVacantJobsByZoneByType.get(ids[id]), alpha)));
            workplace = select(probs, ids);
        } else {
            workplace[0] = -2;
        }
        return workplace;
    }


    private void shuffleWorkers(){

        workerArrayList = new ArrayList<>();
        //All employed persons look for employment, regardless they have already assigned one. That's why also workplace and jobTAZ are set to -1
        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()){
            if (pp.getOccupation() == Occupation.EMPLOYED){
                workerArrayList.add(pp);
                pp.setWorkplace(-1);
            }
        }
        Collections.shuffle(workerArrayList);
        assignedJobs = 0;
    }


    private void identifyVacantJobsByZoneType() {

        logger.info("  Identifying vacant jobs by zone");
        Collection<Job> jobs = dataContainer.getJobDataManager().getJobs();
        TableDataSet jobsByTaz = PropertiesSynPop.get().main.jobsByTaz;

        jobStringTypes = PropertiesSynPop.get().main.jobStringType;
        jobIntTypes = new HashMap<>();
        for (int i = 0; i < PropertiesSynPop.get().main.jobStringType.length; i++) {
            jobIntTypes.put(PropertiesSynPop.get().main.jobStringType[i], i);
        }
        tazIds = dataSetSynPop.getTazs().stream().mapToInt(i -> i).toArray();

        idVacantJobsByZoneType = new HashMap<>();
        numberVacantJobsByType = new HashMap<>();
        idZonesVacantJobsByType = new HashMap<>();
        numberZonesByType = new HashMap<>();
        numberVacantJobsByZoneByType = new HashMap<>();
        jobIntTypes = new HashMap<>();
        for (int i = 0; i < PropertiesSynPop.get().main.jobStringType.length; i++) {
            jobIntTypes.put(PropertiesSynPop.get().main.jobStringType[i], i);
        }
        int[] cellsID = PropertiesSynPop.get().main.cellsMatrix.getColumnAsInt("ID_cell");

        //create the counter hashmaps

        for (int i = 0; i < PropertiesSynPop.get().main.jobStringType.length; i++){
            int type = jobIntTypes.get(PropertiesSynPop.get().main.jobStringType[i]);
            numberZonesByType.put(type,0);
            numberVacantJobsByType.put(type,0);
            for (int taz : jobsByTaz.getColumnAsInt("taz")){
                numberVacantJobsByZoneByType.put(type + taz * 100, 0);
            }
        }
        //get the totals
        int count = 0;
        for (Job jj: jobs) {
            //set all jobs vacant to allocate them
            jj.setWorkerID(-1);
            int type = jobIntTypes.get(jj.getType());
            int typeZone = type + jj.getZoneId() * 100;
            //update the set of zones that have ID
            if (numberVacantJobsByZoneByType.get(typeZone) == 0){
                numberZonesByType.put(type, numberZonesByType.get(type) + 1);
            }
            //update the number of vacant jobs per job type
            numberVacantJobsByType.put(type, numberVacantJobsByType.get(type) + 1);
            numberVacantJobsByZoneByType.put(typeZone, numberVacantJobsByZoneByType.get(typeZone) + 1);
            count++;

        }
        logger.info("Number of vacant jobs " + count);

        //create the IDs Hashmaps and reset the counters
        for (String jobType : PropertiesSynPop.get().main.jobStringType){
            int type = jobIntTypes.get(jobType);
            int[] dummy = SiloUtil.createArrayWithValue(numberZonesByType.get(type),0);
            idZonesVacantJobsByType.put(type,dummy);
            numberZonesByType.put(type,0);
            for (int taz : jobsByTaz.getColumnAsInt("taz")){
                int typeZone = type + taz * 100;
                int[] dummy2 = SiloUtil.createArrayWithValue(numberVacantJobsByZoneByType.get(typeZone), 0);
                idVacantJobsByZoneType.put(typeZone, dummy2);
                numberVacantJobsByZoneByType.put(typeZone, 0);
            }
        }
        //fill the Hashmaps with IDs
        for (Job jj: jobs) {
            //all jobs are vacant in this step of the synthetic population
            int type = jobIntTypes.get(jj.getType());
            int typeZone = jobIntTypes.get(jj.getType()) + jj.getZoneId() * 100;
            //update the list of job IDs per zone and job type
            int [] previousJobIDs = idVacantJobsByZoneType.get(typeZone);
            previousJobIDs[numberVacantJobsByZoneByType.get(typeZone)] = jj.getId();
            idVacantJobsByZoneType.put(typeZone,previousJobIDs);
            //update the set of zones that have ID
            if (numberVacantJobsByZoneByType.get(typeZone) == 0){
                int[] previousZones = idZonesVacantJobsByType.get(type);
                previousZones[numberZonesByType.get(type)] = typeZone;
                idZonesVacantJobsByType.put(type,previousZones);
                numberZonesByType.put(type, numberZonesByType.get(type) + 1);
            }
            //update the number of vacant jobs per job type
            numberVacantJobsByZoneByType.put(typeZone, numberVacantJobsByZoneByType.get(typeZone) + 1);

        }
    }


    private void updateMaps(int selectedJobType, int[] zoneType){

       numberVacantJobsByZoneByType.put(zoneType[0], numberVacantJobsByZoneByType.get(zoneType[0]) - 1);
        numberVacantJobsByType.put(selectedJobType, numberVacantJobsByType.get(selectedJobType) - 1);
        if (numberVacantJobsByZoneByType.get(zoneType[0]) < 1) {
            idZonesVacantJobsByType.get(selectedJobType)[zoneType[1]] = idZonesVacantJobsByType.get(selectedJobType)[numberZonesByType.get(selectedJobType) - 1];
            idZonesVacantJobsByType.put(selectedJobType, idZonesVacantJobsByType.get(selectedJobType));
            numberZonesByType.put(selectedJobType, numberZonesByType.get(selectedJobType) - 1);
            if (numberZonesByType.get(selectedJobType) < 1) {
                int w = 0;
                while (w < PropertiesSynPop.get().main.jobStringType.length & selectedJobType > jobIntTypes.get(jobStringTypes[w])) {
                    w++;
                }
                jobIntTypes.remove(jobStringTypes[w]);
                jobStringTypes[w] = jobStringTypes[jobStringTypes.length - 1];
                jobStringTypes = SiloUtil.removeOneElementFromZeroBasedArray(jobStringTypes, jobStringTypes.length - 1);
            }
        }
    }


    public int guessjobType(Gender gender, int educationLevel){
        int jobType = 0;
        float[] cumProbability;
        switch (gender){
            case MALE:
                switch (educationLevel) {
                    case 0:
                        cumProbability = new float[]{0.01853f,0.265805f,0.279451f,0.382040f,0.591423f,0.703214f,0.718372f,0.792528f,0.8353f,1.0f};
                        break;
                    case 1:
                        cumProbability = new float[]{0.01853f,0.265805f,0.279451f,0.382040f,0.591423f,0.703214f,0.718372f,0.792528f,0.8353f,1.0f};
                        break;
                    case 2:
                        cumProbability = new float[]{0.025005f,0.331942f,0.355182f,0.486795f,0.647928f,0.0748512f,0.779124f,0.838452f,0.900569f,1f};
                        break;
                    case 3:
                        cumProbability = new float[]{0.008533f,0.257497f,0.278324f,0.323668f,0.39151f,0.503092f,0.55153f,0.588502f,0.795734f,1f};
                        break;
                    case 4:
                        cumProbability = new float[]{0.004153f,0.154197f,0.16906f,0.19304f,0.246807f,0.347424f,0.387465f,0.418509f,0.4888415f,1f};
                        break;
                    default: cumProbability = new float[]{0.025005f,0.331942f,0.355182f,0.486795f,0.647928f,0.0748512f,0.779124f,0.838452f,0.900569f,1f};
                }
                break;
            case FEMALE:
                switch (educationLevel) {
                    case 0:
                        cumProbability = new float[]{0.012755f,0.153795f,0.159108f,0.174501f,0.448059f,0.49758f,0.517082f,0.616346f,0.655318f,1f};
                        break;
                    case 1:
                        cumProbability = new float[]{0.012755f,0.153795f,0.159108f,0.174501f,0.448059f,0.49758f,0.517082f,0.616346f,0.655318f,1f};
                        break;
                    case 2:
                        cumProbability = new float[]{0.013754f,0.137855f,0.145129f,0.166915f,0.389282f,0.436095f,0.479727f,0.537868f,0.603158f,1f};
                        break;
                    case 3:
                        cumProbability = new float[]{0.005341f,0.098198f,0.109149f,0.125893f,0.203838f,0.261698f,0.314764f,0.366875f,0.611298f,1f};
                        break;
                    case 4:
                        cumProbability = new float[]{0.002848f,0.061701f,0.069044f,0.076051f,0.142332f,0.197382f,0.223946f,0.253676f,0.327454f,1f};
                        break;
                    default: cumProbability = new float[]{0.013754f,0.137855f,0.145129f,0.166915f,0.389282f,0.436095f,0.479727f,0.537868f,0.603158f,1f};
                }
                break;
            default: cumProbability = new float[]{0.025005f,0.331942f,0.355182f,0.486795f,0.647928f,0.0748512f,0.779124f,0.838452f,0.900569f,1f};
        }
        float threshold = SiloUtil.getRandomNumberAsFloat();
        for (int i = 0; i < cumProbability.length; i++) {
            if (cumProbability[i] > threshold) {
                return i;
            }
        }
        return cumProbability.length - 1;

    }


    public static int[] select (double[] probabilities, int[] id) {
        // select item based on probabilities (for zero-based float array)
        double sumProb = Arrays.stream(probabilities).sum();
        int[] results = new int[2];
        double selPos = sumProb * SiloUtil.getRandomNumberAsFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                results[0] = id[i];
                results[1] = i;
                return results;
            }
        }
        results[0] = id[probabilities.length - 1];
        results[1] = probabilities.length - 1;
        return results;
    }
}
