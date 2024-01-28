package se.thorburn.trailcam;

import com.google.gson.JsonElement;

import java.io.IOException;

public interface ObservationDataService {
    void load(String name, String sourceUrl) throws IOException;

    JsonElement get(String name);
}
