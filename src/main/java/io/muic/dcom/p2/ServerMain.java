package io.muic.dcom.p2;

import spark.Route;
import static spark.Spark.*;
import com.google.gson.Gson;

import java.util.List;

public class ServerMain {
    //Configuration --> use thread = 2*number of processors
    static class Config {
        public static final int DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors()*2;
        public static final int DEFAULT_PORT = 9090;
    }

    public static void main(String args[]) {
        // Spark server setup
        threadPool(Config.DEFAULT_NUM_THREADS);
        port(Config.DEFAULT_PORT);

        // setup data store
        DataModel model = new DataModel();

        // routes
        post("/events", handlePostObserve(model));
        get("/trail/:parcelId", handleGetParcelTrail(model));
        get("/stopCount/:stationId", handleGetStopCount(model));
    }

    private static Route handleGetStopCount(DataModel model) {
        return (request, response) -> {
            String stationId = request.params("stationId");

            if (null != stationId && stationId.length()>0) {
                return model.getStopCount(stationId);
            }
            else {
                String errorMsg = "Invalid stationId";
                halt(400, errorMsg);
                return errorMsg;
            }
        };
    }

    private static Route handleGetParcelTrail(DataModel model) {
        return (request, response) -> {
            String parcelId = request.params("parcelId");
            if (null != parcelId && parcelId.length() > 0) {
//                List<DataModel.ParcelObserved> trail =
                return model.getParcelTrail(parcelId);
            }
            else {
                String errorMsg = "Invalid parcelId";
                halt(400, errorMsg);
                return errorMsg;
            }
        };
    }

    private static Route handlePostObserve(DataModel model) {
        return (request, response) -> {
            String parcelId = request.headers("parcelId");
            String stationId = request.headers("stationId");
            String timestampStr = request.headers("timestamp");
            Long timestamp = null;
            try { timestamp = Long.parseLong(timestampStr); }
            catch (NumberFormatException e) {
                halt(400, "Invalid timestamp.");
            }
            // validate input
            if (null != parcelId && null != stationId &&
                    parcelId.length() > 0 && stationId.length() >0) {
                model.postObserve(parcelId, stationId, timestamp);
                response.status(200);
            } else halt(400);
            return "OK";
        };
    }
}
