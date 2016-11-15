package io.muic.dcom.p2;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class DataModel {
    public static class DataConfig {
        public static final int DEFAULT_NUM_THREADS = 4;
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

    public static class ParcelObserved{
        private String parcelId;
        private String stationId;
        private long timeStamp;
        ParcelObserved(String parcelId_, String stationId_, long ts_) {
            this.parcelId = parcelId_;
            this.stationId = stationId_;
            this.timeStamp = ts_;
        }
        public String getParcelId() { return parcelId; }
        public String getStationId() { return stationId; }
        public long getTimeStamp() { return timeStamp; }
    }
    private ConcurrentHashMap<String,ArrayList<ParcelObserved>>[] parcelTrailWriter;
    private ConcurrentHashMap<String,Integer>[] stationCountWriter;
    private ConcurrentHashMap<String,ArrayList<ParcelObserved>>[] parcelTrailGetter;
    private ConcurrentHashMap<String,Integer>[] stationCountGetter;

    DataModel() {
        parcelTrailWriter = new ConcurrentHashMap[DataConfig.SIZE];
        stationCountWriter = new ConcurrentHashMap[DataConfig.SIZE];
        parcelTrailGetter = new ConcurrentHashMap[DataConfig.SIZE];
        stationCountGetter = new ConcurrentHashMap[DataConfig.SIZE];
        for(int i = 0; i < DataConfig.SIZE; i++){
            parcelTrailWriter[i] = new ConcurrentHashMap<>();
            stationCountWriter[i] = new ConcurrentHashMap<>();
            parcelTrailGetter[i] = new ConcurrentHashMap<>();
            stationCountGetter[i] = new ConcurrentHashMap<>();
        }
    }

    public void postObserve(String parcelId, String stationId, long timestamp) {
        ParcelObserved newp = new ParcelObserved(parcelId,stationId,timestamp);
        int s = extractSlot(parcelId);
        addParcelTrail(s,parcelId,newp);
        incrementStationStopCount(s,stationId);
        ArrayList<ParcelObserved> t = getPath(s,parcelId);
        Integer stopCount = getStationStopCount(s,stationId);
        replace(s,parcelId,t);
        replace(s,stationId,stopCount);
    }

    public ArrayList<ParcelObserved> getPath(Integer slot, String id){
        ArrayList<ParcelObserved> trail = (ArrayList<ParcelObserved>) parcelTrailWriter[slot].get(id).clone();
        return trail;
    }
    public Integer getStationStopCount(Integer slot, String id){
        return stationCountWriter[slot].get(id);
    }
    public void replace(Integer slot, String id, Integer stopCount){
        stationCountGetter[slot].put(id,stopCount);
    }
    public void replace(Integer slot,String id, ArrayList<ParcelObserved> trail){
        parcelTrailGetter[slot].put(id,trail);
    }

    public int extractSlot(String id){
        return DataConfig.PARCEL_PREFIX.get(String.valueOf(id.charAt(0)).toLowerCase());
    }

    public void addParcelTrail(int slot, String parcelId, ParcelObserved parcelObserved){
        if(!parcelTrailWriter[slot].containsKey(parcelId)){parcelTrailWriter[slot].put(parcelId, new ArrayList<>());}
        parcelTrailWriter[slot].get(parcelId).add(parcelObserved);

    }

    public void incrementStationStopCount(int slot, String stationId){
        if(!stationCountWriter[slot].containsKey(stationId)){stationCountWriter[slot].put(stationId, 0);}
        int toPut = stationCountWriter[slot].get(stationId) + 1;
        stationCountWriter[slot].put(stationId,toPut);
    }

    public List<ParcelObserved> getParcelTrail(String parcelId) {
        int slot = extractSlot(parcelId);
        if(!parcelTrailGetter[slot].containsKey(parcelId)){return DataConfig.DEFAULT_TRAIL;}
        else{return parcelTrailGetter[slot].get(parcelId);}
    }
    public long getStopCount(String stationId) {
        int slot = extractSlot(stationId);
        if(!stationCountGetter[slot].containsKey(stationId)){return 0;}
        else{return stationCountGetter[slot].get(stationId);}
    }
}

