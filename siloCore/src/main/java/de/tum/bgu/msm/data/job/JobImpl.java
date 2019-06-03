package de.tum.bgu.msm.data.job;

import de.tum.bgu.msm.data.MicroLocation;
import org.locationtech.jts.geom.Coordinate;

public class JobImpl implements Job, MicroLocation {

    private final int id;
    private int workerId;
    private final String type;
    private Coordinate coordinate;
    private int zoneId;

    JobImpl (int id, int zoneId, Coordinate coordinate, int workerId, String type) {
        this.id = id;
        this.coordinate = coordinate;
        this.workerId = workerId;
        this.type = type;
        this.zoneId = zoneId;
    }

    @Override
    public int getId () {
        return id;
    }

    @Override
    public int getWorkerId() {
        return workerId;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public void setWorkerID(int personID) {
        this.workerId = personID;
    }

    @Override
    public String toString() {
        return "Attributes of job       " + id
//                + "\nLocated in zone         " + zone
                + "\nLocated at         " + coordinate.toString() // TODO implement toString methods
                + "\nFilled by person        " + workerId
                + "\nJob type                " + type;
    }

    @Override
    public int getZoneId() {
        return this.zoneId;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Job && ((Job) o).getId() == this.id;
    }
}
