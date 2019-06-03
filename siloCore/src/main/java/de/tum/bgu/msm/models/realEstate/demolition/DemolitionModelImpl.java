package de.tum.bgu.msm.models.realEstate.demolition;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.Dwelling;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.events.impls.realEstate.DemolitionEvent;
import de.tum.bgu.msm.models.AbstractModel;
import de.tum.bgu.msm.models.relocation.migration.InOutMigrationImpl;
import de.tum.bgu.msm.models.relocation.moves.MovesModelImpl;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simulates demolition of dwellings
 * Author: Rolf Moeckel, PB Albuquerque
 * Created on 8 January 2010 in Rhede
 **/

public class DemolitionModelImpl extends AbstractModel implements DemolitionModel {

    private final static Logger logger = Logger.getLogger(DemolitionModelImpl.class);

    private final MovesModelImpl moves;
    private final InOutMigrationImpl inOutMigration;
    private final DemolitionStrategy strategy;

    private int currentYear = -1;
    private int forcedOutmigrationByDemolition;

    public DemolitionModelImpl(DataContainer dataContainer, MovesModelImpl moves,
                               InOutMigrationImpl inOutMigration, Properties properties,
                               DemolitionStrategy strategy) {
        super(dataContainer, properties);
        this.moves = moves;
        this.inOutMigration = inOutMigration;
//        final Reader reader;
//        switch (properties.main.implementation) {
//            case MUNICH:
//                reader = new InputStreamReader(this.getClass().getResourceAsStream("DemolitionCalc"));
//                break;
//            case MARYLAND:
//                reader = new InputStreamReader(this.getClass().getResourceAsStream("DemolitionCalc"));
//                break;
//            case PERTH:
//                reader = new InputStreamReader(this.getClass().getResourceAsStream("DemolitionCalc"));
//                break;
//            case KAGAWA:
//            case CAPE_TOWN:
//            default:
//                throw new RuntimeException("DemolitionModel implementation not applicable for " + properties.main.implementation);
//        }
        this.strategy = strategy;
    }

    @Override
    public void setup() {

    }

    @Override
    public void prepareYear(int year) {
        forcedOutmigrationByDemolition = 0;
    }

    @Override
    public Collection<DemolitionEvent> getEventsForCurrentYear(int year) {
        currentYear = year;
        final List<DemolitionEvent> events = new ArrayList<>();
        for (Dwelling dwelling : dataContainer.getRealEstateDataManager().getDwellings()) {
            events.add(new DemolitionEvent(dwelling.getId()));
        }
        return events;
    }

    @Override
    public boolean handleEvent(DemolitionEvent event) {
        Dwelling dd = dataContainer.getRealEstateDataManager().getDwelling(event.getDwellingId());
        if (dd != null) {
            if (SiloUtil.getRandomNumberAsDouble() < strategy.calculateDemolitionProbability(dd, currentYear)) {
                return demolishDwelling(dd);
            }
        }
        return false;
    }

    @Override
    public void endYear(int year) {
        if (forcedOutmigrationByDemolition > 0) {
            logger.warn("  Encountered " + forcedOutmigrationByDemolition + " cases " +
                    "where a household had to outmigrate because its dwelling was demolished and no other vacant dwelling could be found.");
        }

    }

    @Override
    public void endSimulation() {

    }

    private boolean demolishDwelling(Dwelling dd) {
        int dwellingId = dd.getId();
        int hhId = dd.getResidentId();
        Household hh = dataContainer.getHouseholdDataManager().getHouseholdFromId(hhId);
        if (hh != null) {
            moveOutHousehold(dwellingId, hh);
        } else {
            dataContainer.getRealEstateDataManager().removeDwellingFromVacancyList(dwellingId);
        }
        dataContainer.getRealEstateDataManager().removeDwelling(dwellingId);
        if (dwellingId == SiloUtil.trackDd) {
            SiloUtil.trackWriter.println("Dwelling " +
                    dwellingId + " was demolished.");
        }
        return true;
    }

    private void moveOutHousehold(int dwellingId, Household hh) {
        int idNewDD = moves.searchForNewDwelling(hh);
        if (idNewDD > 0) {
            moves.moveHousehold(hh, -1, idNewDD);  // set old dwelling ID to -1 to avoid it from being added to the vacancy list
        } else {
            inOutMigration.outMigrateHh(hh.getId(), true);
            dataContainer.getRealEstateDataManager().removeDwellingFromVacancyList(dwellingId);
            forcedOutmigrationByDemolition++;
        }
    }
}

