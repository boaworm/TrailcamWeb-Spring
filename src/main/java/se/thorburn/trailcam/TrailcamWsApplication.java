package se.thorburn.trailcam;

import com.google.gson.*;
import org.joda.time.LocalDate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

@SpringBootApplication
@RestController
public class TrailcamWsApplication {

	private JsonArray m_oklahomaObservations = new JsonArray();
	private JsonArray m_deerObservations = new JsonArray();
	private JsonArray m_manuallySortedClasses = new JsonArray();
	private JsonArray m_timeSeries = new JsonArray();

	public TrailcamWsApplication(){

		// refreshDataFiles();

	}

	public static void main(String[] args) {
		SpringApplication.run(TrailcamWsApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	@GetMapping("getFirstAndLastDate")
	public String getFirstAndLastDate(){
		LocalDate oldestDate = LocalDate.now();
		LocalDate newestDate = LocalDate.now();

		return null;
	}

	@GetMapping("/getDataFile")
	public String getDataFile(@RequestParam(value = "name") String fileName) {
		switch (fileName){
			case "oklahomaObservations" :
				return new Gson().toJson(m_oklahomaObservations);
			case "deerObservations" :
				return new Gson().toJson(m_deerObservations);
			case "manuallySortedClasses" :
				return new Gson().toJson(m_manuallySortedClasses);
			case "timeSeries":
				return new Gson().toJson(m_timeSeries);
			default:
				return "Unsupported file: " + fileName;
		}
	}



	@EventListener(ApplicationReadyEvent.class)
	@GetMapping("/refreshDataFiles")
	public String refreshDataFiles() {
		refreshData();
		return "OK";
	}

	private void refreshData(){
		m_oklahomaObservations = refreshSingleDataFile("https://www.thorburn.se/trailcam/oklahoma_observations.json");
		m_deerObservations = refreshSingleDataFile("https://www.thorburn.se/trailcam/deer_observations.json");
		m_manuallySortedClasses = refreshSingleDataFile("https://www.thorburn.se/trailcam/manually_sorted_categories.json");
		m_timeSeries = refreshSingleDataFile("https://www.thorburn.se/trailcam/time_series_dimension.json");
	}

	private JsonArray refreshSingleDataFile(String sourceUrl){
		System.out.println("Loading data file from [" + sourceUrl + "]");
		try {
			URL url = new URL(sourceUrl);
			URLConnection request = url.openConnection();
			request.connect();
			JsonElement root = new JsonParser().parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element

			if(root.isJsonArray()){
				return root.getAsJsonArray();
			}else if(root.isJsonObject()){
				System.out.println("Unsupported type: JsonObject (only support arrays for now)");
			}else{
				System.out.println("Unclear what we have...");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

}
