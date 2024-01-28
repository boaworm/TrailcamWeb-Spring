package se.thorburn.trailcam;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
		LocalDate oldestDate = null; // LocalDate.now();
		LocalDate newestDate = null; // LocalDate.now();

		JsonElement observations = dataService.get("animalObservations");
		if(observations == null){
			return "ERROR";
		}

		for(int i = 0; i< dataService.get("animalObservations").getAsJsonArray().size(); i++){
			JsonObject obj = dataService.get("animalObservations").getAsJsonArray().get(i).getAsJsonObject();
			int year = obj.get("year").getAsInt();
			int month = obj.get("month").getAsInt();
			int day = obj.get("day").getAsInt();
			LocalDate tmpDate = new LocalDate(year, month, day);

			if(oldestDate == null){
				oldestDate = tmpDate;
			} else if (tmpDate.compareTo(oldestDate) < 0){
				oldestDate = tmpDate;
			}

			if(newestDate == null){
				newestDate = tmpDate;
			} else 	if(tmpDate.compareTo(newestDate) > 0){
				newestDate = tmpDate;
			}
		}

		JsonObject returnedObject = new JsonObject();
		returnedObject.addProperty("oldestDate", oldestDate.toString());
		returnedObject.addProperty("newestDate", newestDate.toString());
		return returnedObject.toString();
	}

	@GetMapping("getSortedListOfYears")
	public String getSortedListOfYears() {
		Set<String> yearSet = new HashSet<String>();
		JsonArray observations = dataService.get("animalObservations").getAsJsonArray();
		for(int i = 0; i < observations.size(); i++){
			JsonObject asJsonObject = observations.get(i).getAsJsonObject();
			String year = asJsonObject.get("year").getAsString();
			yearSet.add(year);
		}
		ArrayList<String> arrayList = new ArrayList<String>(yearSet);
		Collections.sort(arrayList);
		return "[" + String.join(",",arrayList) + "]";
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
			dataService.load("animalObservations", "https://www.thorburn.se/trailcam/oklahoma_observations.json");
			dataService.load("deerObservations", "https://www.thorburn.se/trailcam/deer_observations.json");
			dataService.load("manuallySortedObservations", "https://www.thorburn.se/trailcam/manually_sorted_categories.json");
			dataService.load("timeSeries", "https://www.thorburn.se/trailcam/time_series_dimension.json");
			dataService.load("manuallyClassifiedObservations", "https://www.thorburn.se/trailcam/manually_classified.json");
		}catch(IOException e){
			System.out.println("Failed to refresh data: " + e);
		}
		return "Successfully refreshed data files";
	}

	@GetMapping("/getMLvsManualComparison")
	public String getMLvsManualComparison() {
		JsonArray deerObservations = this.dataService.get("deerObservations").getAsJsonArray();
		JsonArray manuallyClassifiedObservations = this.dataService.get("manuallyClassifiedObservations").getAsJsonArray();

		// Build up a structure with:
		// imageName + deerClassification + deerClassificationConfidence + manualClassification + manualClassificationConfidence

		// Sort into buckets...
		// I want to check how accurate the ML classifications are. Hence
		// foreach deerObservation
		//	 if

		int truePositive = 0;
		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;
		int nonMatching = 0;
		int matching = 0;

		for(int deerIndex = 0; deerIndex < deerObservations.size(); deerIndex++){
			String deerImageName = deerObservations.get(deerIndex).getAsJsonObject().get("image").getAsString();
			// Looking up image in manual classification set
			JsonElement matchingElement = null;
			for(int manualIndex = 0; manualIndex < manuallyClassifiedObservations.size(); manualIndex++){
				String manualObsImageName = manuallyClassifiedObservations.get(manualIndex).getAsJsonObject().get("image").getAsString();
				if(deerImageName.compareTo(manualObsImageName) == 0){
					matchingElement = manuallyClassifiedObservations.get(manualIndex);
					break;
				}
			}

			if(matchingElement == null){
				nonMatching++;
			}else{
				String mlClass = deerObservations.get(deerIndex).getAsJsonObject().get("classification").getAsString();
				String humanClass = matchingElement.getAsJsonObject().get("classification").getAsString();
				matching++;
				if("1.Deer".compareTo(mlClass) == 0){
					if("1.Deer".compareTo(humanClass) == 0){
						truePositive++;
					}else{
						falsePositive++;
					}
				}else{
					if("1.Deer".compareTo(humanClass) == 0){
						falseNegative++;
					}else{
						trueNegative++;
					}
				}
			}
		}

		JsonArray arr = new JsonArray();
		JsonObject row = new JsonObject();
		row.addProperty("correctness", "Correct");
		row.addProperty("theGroup", "TruePositive");
		row.addProperty("observations", truePositive);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Correct");
		row.addProperty("theGroup", "TrueNegative");
		row.addProperty("observations", trueNegative);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Correct");
		row.addProperty("theGroup", "FalsePositive");
		row.addProperty("observations", 0);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Correct");
		row.addProperty("theGroup", "FalseNegative");
		row.addProperty("observations", 0);
		arr.add(row);

		// Incorrect stuff
		row = new JsonObject();
		row.addProperty("correctness", "Incorrect");
		row.addProperty("theGroup", "TruePositive");
		row.addProperty("observations", 0);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Incorrect");
		row.addProperty("theGroup", "TrueNegative");
		row.addProperty("observations", 0);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Incorrect");
		row.addProperty("theGroup", "FalsePositive");
		row.addProperty("observations", falsePositive);
		arr.add(row);

		row = new JsonObject();
		row.addProperty("correctness", "Incorrect");
		row.addProperty("theGroup", "FalseNegative");
		row.addProperty("observations", falseNegative);
		arr.add(row);

		return new Gson().toJson(arr);
	}
}
