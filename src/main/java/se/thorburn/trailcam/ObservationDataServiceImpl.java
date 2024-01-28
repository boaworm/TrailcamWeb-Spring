package se.thorburn.trailcam;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

@Service
@Qualifier("DataService")
public class ObservationDataServiceImpl implements ObservationDataService {
    @Override
    public void load(String name, String sourceUrl) throws IOException {
        System.out.println("Loading URL: " + sourceUrl);
        URL url = new URL(sourceUrl);
        URLConnection request = url.openConnection();
        request.connect();
        JsonElement element = new JsonParser().parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        m_data.put(name, element);
    }

    private Map<String, JsonElement> m_data = new HashMap<String, JsonElement>();

    @Override
    public JsonElement get(String name) {
        if(m_data.containsKey(name)){
            return m_data.get(name);
        }else {
            return null;
        }
    }
}
