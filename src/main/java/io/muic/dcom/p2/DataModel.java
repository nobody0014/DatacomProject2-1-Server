package io.muic.dcom.p2;

import javax.xml.crypto.Data;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import com.google.gson.Gson;

public class DataModel {
    //Config of the datamodel
    public static class DataConfig {
        public static final int SIZE = 36;
        public static final String DEFAULT_TRAIL = "";
        //Use this to get the prefix to the slot in the static array, same for both Stopcount and Parceltrail
        public static final ConcurrentHashMap<String,Integer> PARCEL_PREFIX = createParcelPrefix();
        public static ConcurrentHashMap<String,Integer> createParcelPrefix(){
            ConcurrentHashMap<String, Integer> bucket  = new ConcurrentHashMap<>();
            for(int i = 0; i < 10; i++){bucket.put(String.valueOf(i),i);}
            for(short i = 87; i < 123;i++){bucket.put(String.valueOf((char) i),i-77);}
            return bucket;
        }
    }
    //Make parcelObserved comparable to make the ConcurrentSkipListSet sortable
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
            int compareResult = Long.compare(this.getTimeStamp(),o.getTimeStamp());
            if(compareResult == 0){
                compareResult = this.stationId.compareTo(o.getStationId());
            }
            return compareResult;
        }
        public String getParcelId() { return parcelId; }
        public String getStationId() { return stationId; }
        public long getTimeStamp() { return timeStamp; }
    }
    //
    ConcurrentHashMap<String,ConcurrentSkipListSet<ParcelObserved>>[] parcelTrailWriter;
    ConcurrentHashMap<String,Long>[] stationCountWriter;
    //Initialised  parcel writer and stationCount
    DataModel() {
        parcelTrailWriter = new ConcurrentHashMap[DataConfig.SIZE];
        stationCountWriter = new ConcurrentHashMap[DataConfig.SIZE];
        for(int i = 0; i < DataConfig.SIZE; i++){
            parcelTrailWriter[i] = new ConcurrentHashMap<>();
            stationCountWriter[i] = new ConcurrentHashMap<>();
        }
    }

    //Posting parcels --> get the id for station and parcel and add them.
    public void postObserve(String parcelId, String stationId, long timestamp) {
        ParcelObserved newp = new ParcelObserved(parcelId,stationId,timestamp);
        int parcelSlot = extractSlot(parcelId);
        int stationSlot = extractSlot(stationId);
        addParcelTrail(parcelSlot,parcelId,newp);
        incrementStationStopCount(stationSlot,stationId);
    }
    //get the slot
    public int extractSlot(String id){
        return DataConfig.PARCEL_PREFIX.get(String.valueOf(id.charAt(0)).toLowerCase());
    }


    public void addParcelTrail(int slot, String parcelId, ParcelObserved parcelObserved){
        parcelTrailWriter[slot].putIfAbsent(parcelId,new ConcurrentSkipListSet<>());
        parcelTrailWriter[slot].get(parcelId).add(parcelObserved);
    }

    public void incrementStationStopCount(int slot, String stationId){
        stationCountWriter[slot].putIfAbsent(stationId, (long) 0 );
        long toPut = stationCountWriter[slot].get(stationId) + 1;
        stationCountWriter[slot].put(stationId,toPut);
    }

    public String getParcelTrail(String parcelId) {
        int slot = extractSlot(parcelId);
        if(!parcelTrailWriter[slot].containsKey(parcelId)){
            return DataConfig.DEFAULT_TRAIL;
        }
        else{
            ArrayList<ParcelObserved> pp = new ArrayList<>(parcelTrailWriter[slot].get(parcelId));
            return  (new Gson()).toJson(pp);
        }
    }
    public long getStopCount(String stationId) {
        int slot = extractSlot(stationId);
        if(!stationCountWriter[slot].containsKey(stationId)){return 0;}
        else{return stationCountWriter[slot].get(stationId);}
    }
}

