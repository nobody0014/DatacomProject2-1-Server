package io.muic.dcom.p2;

import javax.xml.crypto.Data;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class DataModel {
    public static class DataConfig {
        public static final int SIZE = 36;
        public static final ArrayList<ParcelObserved> DEFAULT_TRAIL = new ArrayList<>();
        public static final ConcurrentHashMap<String,Integer> PARCEL_PREFIX = createParcelPrefix();
        public static ConcurrentHashMap<String,Integer> createParcelPrefix(){
            ConcurrentHashMap<String, Integer> bucket  = new ConcurrentHashMap<>();
            for(int i = 0; i < 10; i++){bucket.put(String.valueOf(i),i);}
            for(short i = 87; i < 123;i++){bucket.put(String.valueOf((char) i),i-77);}
            return bucket;
        }
    }

    public static class ParcelObserved implements Comparable<ParcelObserved>{
        private String parcelId;
        private String stationId;
        private long timeStamp;
        ParcelObserved(String parcelId_, String stationId_, long ts_) {
            this.parcelId = parcelId_;
            this.stationId = stationId_;
            this.timeStamp = ts_;
        }

        @Override
        public int compareTo(ParcelObserved o) {
            return Long.compare(this.getTimeStamp(),o.getTimeStamp());
        }
        public String getParcelId() { return parcelId; }
        public String getStationId() { return stationId; }
        public long getTimeStamp() { return timeStamp; }
    }
    ConcurrentHashMap<String,ConcurrentSkipListSet<ParcelObserved>>[] parcelTrailWriter;
    ConcurrentHashMap<String,Long>[] stationCountWriter;

    DataModel() {
        parcelTrailWriter = new ConcurrentHashMap[DataConfig.SIZE];
        stationCountWriter = new ConcurrentHashMap[DataConfig.SIZE];
        for(int i = 0; i < DataConfig.SIZE; i++){
            parcelTrailWriter[i] = new ConcurrentHashMap<>();
            stationCountWriter[i] = new ConcurrentHashMap<>();
        }
    }

    public void postObserve(String parcelId, String stationId, long timestamp) {
        ParcelObserved newp = new ParcelObserved(parcelId,stationId,timestamp);
        int parcelSlot = extractSlot(parcelId);
        int stationSlot = extractSlot(stationId);
        addParcelTrail(parcelSlot,parcelId,newp);
        incrementStationStopCount(stationSlot,stationId);
    }

    public int extractSlot(String id){
        return DataConfig.PARCEL_PREFIX.get(String.valueOf(id.charAt(0)).toLowerCase());
    }

    public void addParcelTrail(int slot, String parcelId, ParcelObserved parcelObserved){
        if(!parcelTrailWriter[slot].containsKey(parcelId)){
            parcelTrailWriter[slot].put(parcelId, new ConcurrentSkipListSet<>());
        }
        parcelTrailWriter[slot].get(parcelId).add(parcelObserved);
    }

    public void incrementStationStopCount(int slot, String stationId){
        if(!stationCountWriter[slot].containsKey(stationId)){stationCountWriter[slot].put(stationId, (long)0);}
        long toPut = stationCountWriter[slot].get(stationId) + 1;
        stationCountWriter[slot].put(stationId,toPut);
    }

    public ArrayList<ParcelObserved> getParcelTrail(String parcelId) {
        int slot = extractSlot(parcelId);
        if(!parcelTrailWriter[slot].containsKey(parcelId)){
            return DataConfig.DEFAULT_TRAIL;
        }
        else{
            ArrayList<ParcelObserved> pp = new ArrayList<>(parcelTrailWriter[slot].get(parcelId));
            return  pp;
        }
    }
    public long getStopCount(String stationId) {
        int slot = extractSlot(stationId);
        if(!stationCountWriter[slot].containsKey(stationId)){return 0;}
        else{return stationCountWriter[slot].get(stationId);}
    }
}

