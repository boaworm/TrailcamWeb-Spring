package se.thorburn.trailcam;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TrailcamWsApplication.class)
@ExtendWith(MockitoExtension.class)
class TrailcamWsApplicationTests {

	TrailcamWsApplication m_trailcamApplication;
	@Mock
	ObservationDataService mockedDataService;

	@Mock
	JsonElement timeSeriesJsonElement;
	@Mock
	private JsonElement animalObservationsElement;
	@Mock
	private JsonElement deerObservationsElement;
	@Mock
	private JsonElement manuallySortedObservationsElement;
	@Mock
	private JsonElement manuallyClassifiedObservationsElement;

	@BeforeEach
	public void setupMock() throws IOException {
		m_trailcamApplication = new TrailcamWsApplication();
		m_trailcamApplication.setDataService(mockedDataService);

		// Mock time series
		String timeSeries = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("static/time_series_dimension.json"),
				StandardCharsets.UTF_8);
		timeSeriesJsonElement = JsonParser.parseString(timeSeries);

		// Mock observations
		String animalObservations = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("static/oklahoma_observations.json"),
				StandardCharsets.UTF_8);
		animalObservationsElement = JsonParser.parseString(animalObservations);

		String deerObservations = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("static/deer_observations.json"),
				StandardCharsets.UTF_8);
		deerObservationsElement = JsonParser.parseString(deerObservations);

		String manuallySortedObservations = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("static/manually_sorted_categories.json"),
				StandardCharsets.UTF_8);
		manuallySortedObservationsElement = JsonParser.parseString(manuallySortedObservations);

		String manuallyClassifiedObservations = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("static/manually_classified.json"),
				StandardCharsets.UTF_8);
		manuallyClassifiedObservationsElement = JsonParser.parseString(manuallyClassifiedObservations);

		// Mock dataservice response
		Mockito.lenient().when(mockedDataService.get("timeSeries")).thenReturn(timeSeriesJsonElement);
		Mockito.lenient().when(mockedDataService.get("animalObservations")).thenReturn(animalObservationsElement);
		Mockito.lenient().when(mockedDataService.get("deerObservations")).thenReturn(deerObservationsElement);
		Mockito.lenient().when(mockedDataService.get("manuallySortedObservations")).thenReturn(manuallySortedObservationsElement);
		Mockito.lenient().when(mockedDataService.get("manuallyClassifiedObservations")).thenReturn(manuallyClassifiedObservationsElement);
	}

	@AfterEach
	public void teardownMock(){
		m_trailcamApplication = null;
	}

	@Test
	void verify_unsupportedDataYieldsError(){
		String shouldBeError = m_trailcamApplication.getDataFile("undefined");
		assertThat(shouldBeError, equalTo("ERROR"));
	}

	@Test
	void verify_timeSeries(){
		String timeSeries = m_trailcamApplication.getDataFile("timeSeries");
		assertThat(timeSeries, notNullValue());
		assertTrue(timeSeries.startsWith("[{\"date\":\"2023-12-01\""));
	}

	@Test
	void verify_manuallySortedObservations(){
		String manualObs = m_trailcamApplication.getDataFile("manuallySortedObservations");
		assertThat(manualObs, notNullValue());
		assertTrue(manualObs.startsWith("[{\"category\":\"8.Cat\""));
	}

	@Test
	void verify_animalObservations(){
		String animalObs = m_trailcamApplication.getDataFile("animalObservations");
		assertThat(animalObs, notNullValue());
		assertTrue(animalObs.startsWith("[{\"date\":\"2023-12-07\""));
	}

	@Test
	void verify_deerObservations(){
		String deerObs = m_trailcamApplication.getDataFile("deerObservations");
		assertThat(deerObs, notNullValue());
		assertTrue(deerObs.startsWith("[{\"image\":\"TOP_2023-12-07"));
	}

	@Test
	void verify_manualClassification(){
		String manualClassification = m_trailcamApplication.getDataFile("manuallyClassifiedObservations");
		assertThat(manualClassification, notNullValue());
		assertTrue(manualClassification.startsWith("[{\"confidence\":1.0,\"image\":\"HOUSE"));
	}


	@Test
	void verify_oldestNewestDate(){
		String firstAndLastDate = m_trailcamApplication.getFirstAndLastDate();
		assertThat(firstAndLastDate, notNullValue());
		JsonElement el = JsonParser.parseString(firstAndLastDate);
		assertThat(el, notNullValue());
		assertThat(((JsonObject) el).asMap().get("oldestDate").getAsString(), equalTo("2023-12-02"));
		assertThat(((JsonObject) el).asMap().get("newestDate").getAsString(), equalTo("2024-01-23"));
	}

	@Test
	void verify_yearArray(){
		String yearArray = m_trailcamApplication.getSortedListOfYears();
		assertThat(yearArray, equalTo("[2023,2024]"));
	}

	@Test
	void verify_mlVsManual(){
		String deerObs = m_trailcamApplication.getDataFile("deerObservations");
		String manualObs = m_trailcamApplication.getDataFile("manuallyClassifiedObservations");

		assertThat(deerObs, notNullValue());
		assertThat(manualObs, notNullValue());

		String comparisonString = m_trailcamApplication.getMLvsManualComparison();
		assertThat(comparisonString, notNullValue());
		assertThat(comparisonString, equalTo("[{\"correctness\":\"Correct\",\"theGroup\":\"TruePositive\",\"observations\":816},{\"correctness\":\"Correct\",\"theGroup\":\"TrueNegative\",\"observations\":187},{\"correctness\":\"Correct\",\"theGroup\":\"FalsePositive\",\"observations\":0},{\"correctness\":\"Correct\",\"theGroup\":\"FalseNegative\",\"observations\":0},{\"correctness\":\"Incorrect\",\"theGroup\":\"TruePositive\",\"observations\":0},{\"correctness\":\"Incorrect\",\"theGroup\":\"TrueNegative\",\"observations\":0},{\"correctness\":\"Incorrect\",\"theGroup\":\"FalsePositive\",\"observations\":220},{\"correctness\":\"Incorrect\",\"theGroup\":\"FalseNegative\",\"observations\":8}]"));


	}
}
