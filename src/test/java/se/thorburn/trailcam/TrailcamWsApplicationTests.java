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

		// Mock dataservice response
		Mockito.lenient().when(mockedDataService.get("timeSeries")).thenReturn(timeSeriesJsonElement);
		Mockito.lenient().when(mockedDataService.get("oklahomaObservations")).thenReturn(animalObservationsElement);

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
	void verify_oldestNewestDate(){
		String firstAndLastDate = m_trailcamApplication.getFirstAndLastDate();
		assertThat(firstAndLastDate, notNullValue());
		JsonElement el = JsonParser.parseString(firstAndLastDate);
		assertThat(el, notNullValue());
		assertThat(((JsonObject) el).asMap().get("oldestDate").getAsString(), equalTo("2023-12-02"));
		assertThat(((JsonObject) el).asMap().get("newestDate").getAsString(), equalTo("2024-01-27"));
	}

}
