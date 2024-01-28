package se.thorburn.trailcam;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@SpringBootApplication
@RestController
public class TrailcamWsApplication {

	@Autowired()
	@Qualifier("DataService")
	private ObservationDataService dataService;

	public TrailcamWsApplication(){
	}

	public void setDataService(ObservationDataService service){
		this.dataService = service;
	}

	public static void main(String[] args) {
		SpringApplication.run(TrailcamWsApplication.class, args);
	}

	@GetMapping("getFirstAndLastDate")
	public String getFirstAndLastDate(){
		LocalDate oldestDate = LocalDate.now();
		LocalDate newestDate = LocalDate.now();

		JsonElement observations = dataService.get("oklahomaObservations");
		if(observations == null){
			return "ERROR";
		}

		for(int i = 0; i< dataService.get("oklahomaObservations").getAsJsonArray().size(); i++){
			JsonObject obj = dataService.get("oklahomaObservations").getAsJsonArray().get(i).getAsJsonObject();
			int year = obj.get("year").getAsInt();
			int month = obj.get("month").getAsInt();
			int day = obj.get("day").getAsInt();
			LocalDate tmpDate = new LocalDate(year, month, day);
			if(tmpDate.compareTo(oldestDate) < 0){
				oldestDate = tmpDate;
			}
			if(tmpDate.compareTo(newestDate) > 0){
				newestDate = tmpDate;
			}
		}

		JsonObject returnedObject = new JsonObject();
		returnedObject.addProperty("oldestDate", oldestDate.toString());
		returnedObject.addProperty("newestDate", newestDate.toString());
		return returnedObject.toString();
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/getDataFile")
	public String getDataFile(@RequestParam(value = "name") String fileName) {
		JsonElement jsonElement = this.dataService.get(fileName);
		if(jsonElement == null) {
			System.out.println("Error: Attempting to retrieve unsupported data set : " + fileName);
			return "ERROR";
		}else{
			return new Gson().toJson(jsonElement);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	@GetMapping("/refreshDataFiles")
	public String refreshDataFiles() {
		System.out.println("Refreshing data files");
		try {
			dataService.load("oklahomaObservations", "https://www.thorburn.se/trailcam/oklahoma_observations.json");
			dataService.load("deerObservations", "https://www.thorburn.se/trailcam/deer_observations.json");
			dataService.load("manuallySortedObservations", "https://www.thorburn.se/trailcam/manually_sorted_categories.json");
			dataService.load("timeSeries", "https://www.thorburn.se/trailcam/time_series_dimension.json");
		}catch(IOException e){
			System.out.println("Failed to refresh data: " + e);
		}
		return "Successfully refreshed data files";
	}

}
