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
package de.tum.bgu.msm.data.dwelling;

import de.tum.bgu.msm.data.MicroLocation;
import org.locationtech.jts.geom.Coordinate;

/**
 * @author Greg Erhardt
 * Created on Dec 2, 2009
 */
public final class DwellingImpl implements Dwelling, MicroLocation {

    //Attributes that must be initialized when one dwelling is generated
    private final int id;
    private final int zoneId;
    private final DwellingType type;
    private final int bedrooms;
    private final int yearBuilt;
    private int hhId;
    private int quality;
    private int price;
    private float restriction;
    //Attributes that could be additionally defined from the synthetic population. Remember to use "set"
    private int buildingSize = 0;
    private int floorSpace = 0;
    private DwellingUsage usage = DwellingUsage.GROUP_QUARTER_OR_DEFAULT;
    private int yearConstructionDE = 0;
    private Coordinate coordinate;


    DwellingImpl(int id, int zoneId, Coordinate coordinate, int hhId, DwellingType type, int bedrooms, int quality, int price, float restriction,
                 int year) {
        this.id = id;
        this.zoneId = zoneId;
        this.coordinate = coordinate;
        this.hhId = hhId;
        this.type = type;
        this.bedrooms = bedrooms;
        this.quality = quality;
        this.price = price;
        this.restriction = restriction;
        this.yearBuilt = year;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public int getZoneId() {
        return zoneId;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    public int getResidentId() {
        return hhId;
    }

    @Override
    public int getPrice() {
        return price;
    }

    @Override
    public DwellingType getType() {
        return type;
    }

    @Override
    public int getBedrooms() {
        return bedrooms;
    }

    @Override
    public int getYearBuilt() {
        return yearBuilt;
    }

    @Override
    public float getRestriction() {
        // 0: no restriction, negative value: rent-controlled, positive value: rent-controlled and maximum income of renter
        return restriction;
    }

    @Override
    public void setResidentID(int residentID) {
        this.hhId = residentID;
    }

    @Override
    public void setQuality(int quality) {
        this.quality = quality;
    }

    @Override
    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public void setRestriction(float restriction) {
        // 0: no restriction, negative value: rent-controlled, positive value: rent-controlled and maximum income of renter
        this.restriction = restriction;
    }

    @Override
    public void setFloorSpace(int floorSpace) {
        this.floorSpace = floorSpace;
        //Usable square meters of the dwelling.
        //Numerical value from 1 to 9999
    }

    @Override
    public int getFloorSpace() {
        return floorSpace;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }


    //TODO: magic numbers
    //TODO: use case specific
    @Override
    public void setBuildingSize(int buildingSize) {
        this.buildingSize = buildingSize;
        //Number of dwellings inside the building
        //1: 1 or 2 apartments
        //2: 3 to 6 apartments
        //3: 7 to 12 apartments
        //4: 13 to 20 apartments
        //5: 21 or more apartments
        //0: default
        //There are not supposed to be any "no stated (9)" or "group quarter (-1)" or "moved out (-5)". They are filtered before.
    }

    //TODO: use case specific
    @Override
    public int getBuildingSize() {
        return buildingSize;
    }

    //TODO: use case specific
    @Override
    public void setUsage(DwellingUsage usage) {
        this.usage = usage;
    }

    //TODO: use case specific
    @Override
    public DwellingUsage getUsage() {
        return usage;
    }

    //TODO: magic numbers
    //TODO: use case specific
    @Override
    public void setYearConstructionDE(int yearConstructionDE) {
        this.yearConstructionDE = yearConstructionDE;
        //Dwelling construction year in Germany
        //1: before 1919
        //2: 1919 - 1948
        //3: 1949 - 1978
        //4: 1979-1986
        //5: 1987 - 1990
        //6: 1991 - 2000
        //7: 2001 - 2004
        //8: 2005 - 2008
        //9: 2009 or later
        //There are not supposed to be any "no stated (99)" or "group quarter (-1)" or "moved out (-5)". They are filtered before.
    }

    //TODO: use case specific
    @Override
    public int getYearConstructionDE() {
        return yearConstructionDE;
    }


    @Override
    public String toString() {
        return "Attributes of dwelling  " + id
                +"\nLocated in zone         "+ zoneId
                + "\nLocated at		        " + (coordinate.toString()) // TODO implement toString methods
                + "\nOccupied by household   " + (hhId)
                + "\nDwelling type           " + (type.toString())
                + "\nNumber of bedrooms      " + (bedrooms)
                + "\nQuality (1 low, 4 high) " + (quality)
                + "\nMonthly price in US$    " + (price)
                + "\nAffordable housing      " + (restriction)
                + "\nYear dwelling was built " + (yearBuilt);
    }

}
