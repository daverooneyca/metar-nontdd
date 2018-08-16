package com.saphala.nontdd.metar;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class MetarApp {
	private String apiKey = null;
	private String rawMetar = null;
	private String station = null;
	private String reportTime = null;
	private String winds = null;
	private String visibility = null;
	private String conditions = null;
	private List<String> clouds = null;
	private Object temperature = null;
	private String dewPoint = null;
	private String altimeter = null;
	private String remarks = null;

	private int cloudsEndIndex = 0;
	private int windsStartIndex = 0;
	private int visibilityEndIndex = 0;
	private int windsEndIndex;

	public MetarApp() {
		this.apiKey = System.getenv("CHECKWX-API-KEY");

		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return jacksonObjectMapper.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void retrieveMetarFor(String station) throws Exception {
		try {
			HttpResponse<MetarResult> metarResponse = Unirest.get("https://api.checkwx.com/metar/{station}/")
					.header("accept", "application/json").header("X-API-Key", this.apiKey)
					.routeParam("station", station).asObject(MetarResult.class);

			MetarResult result = metarResponse.getBody();
			
			this.rawMetar = result.getData().get(0);
			
			if(this.rawMetar.toLowerCase().contains("invalid station")) {
				throw new Exception("No results found for station: " + station);
			}

			if(this.rawMetar.toLowerCase().contains("currently unavailable")) {
				throw new Exception("METAR currently unavailable for station: " + station);
			}

			parseRawMetar();

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Unirest.shutdown();
		}

		return;
	}

	public void parseRawMetar() {
		String[] metar = this.rawMetar.split(" ");

		if (metar.length == 0) {
			return;
		}

		List<String> metarList = Arrays.asList(metar);

		this.station = metarList.get(0);
		this.reportTime = metarList.get(1);
		
		this.windsStartIndex = 2;
		
		if(Character.isAlphabetic(metarList.get(2).charAt(0))) {
			windsStartIndex++;
		}

		this.winds = extractWindsFrom(metarList);
		this.visibility = extractVisibilityFrom(metarList);
		this.conditions = extractConditionsFrom(metarList);
		this.clouds = extractCloudsFrom(metarList);
		this.temperature = extractTemperatureFrom(metarList);
		this.dewPoint = extractDewpointFrom(metarList);
		this.altimeter = metarList.get(cloudsEndIndex + 1);
		this.remarks = metarList.subList(cloudsEndIndex + 2, metarList.size()).toString();
	}

	private String extractWindsFrom(List<String> metarList) {
		int startPosition = windsStartIndex;
		int endPosition = windsStartIndex + 1;
		
		List<String> winds = metarList.subList(startPosition, metarList.size());
		
		boolean variableWindsFound = false;
		
		for (String entry : winds) {
			if(entry.contains("V") && Character.isDigit(entry.charAt(0))) {
				variableWindsFound = true;
				break;
			}
			
			endPosition++;
		}
		
		if(variableWindsFound) {
			this.windsEndIndex =  endPosition;
		} else {
			this.windsEndIndex = this.windsStartIndex + 1;
		}

		return metarList.subList(windsStartIndex, windsEndIndex).toString();
	}

	private String extractConditionsFrom(List<String> metarList) {
		int startPosition = findVisibilityEndIndexIn(metarList);
		int endPosition = findCloudsStartIndexIn(metarList);
		
		if (endPosition < startPosition) {
			endPosition = startPosition;
		}
		
		List<String> conditionsList = metarList.subList(startPosition, endPosition);
		
		return conditionsList.toString();
	}

	private String extractVisibilityFrom(List<String> metarList) {
		int startPosition = windsEndIndex;
		int endPosition = startPosition + 1;

		endPosition = findVisibilityEndIndexIn(metarList);
		
		List<String> visibilityList = metarList.subList(startPosition, endPosition);
		
		String visibility = "";
		
		for (String entry : visibilityList) {
			visibility = visibility + entry + " ";
		}
		
		this.visibilityEndIndex = endPosition;

		return visibility.trim();
	}

	private int findVisibilityEndIndexIn(List<String> metarList) {
		int startPosition = windsStartIndex + 1;
		int endPosition = startPosition + 1;
		
		for (String entry : metarList.subList(startPosition, metarList.size())) {
			if (entry.endsWith("SM")) {
				break;
			}
			endPosition++;
		}
		
		return endPosition;
	}

	private String extractTemperatureFrom(List<String> metarList) {
		String tempAndDewpoint = metarList.get(this.cloudsEndIndex);

		return tempAndDewpoint.split("/")[0];
	}

	private String extractDewpointFrom(List<String> metarList) {
		String tempAndDewpoint = metarList.get(this.cloudsEndIndex);

		return tempAndDewpoint.split("/")[1];
	}

	private List<String> extractCloudsFrom(List<String> metarList) {
		int startPosition = findCloudsStartIndexIn(metarList);
		int endPosition = startPosition;

		for (String entry : metarList.subList(startPosition, metarList.size())) {
			char firstChar = entry.charAt(0);
			if (Character.isDigit(firstChar)) {
				break;
			}
			endPosition++;
		}

		this.cloudsEndIndex = endPosition;

		List<String> clouds = metarList.subList(startPosition, endPosition);

		return clouds;
	}

	private int findCloudsStartIndexIn(List<String> metarList) {
		List<String> skyConditionPrefixes = Arrays.asList(
			"VV", "SKC", "CLR", "FEW", "SCT", "BKN", "OVC");

		int startPosition = 0;
		boolean found = false;
		
		for (String entry : metarList) {
			for (String prefix : skyConditionPrefixes) {
				if(entry.startsWith(prefix)) {
					found = true;
					break;
				}
			}
			
			if(found) {
				break;
			}
			
			startPosition++;
		}
		
		return startPosition;
	}

	public int getWindsIndex() {
		return windsStartIndex;
	}

	public int getVisibilityEndIndex() {
		return visibilityEndIndex;
	}

	public String getRawMetar() {
		return this.rawMetar;
	}

	public String getStation() {
		return this.station;
	}

	private String getReportTime() {
		return this.reportTime;
	}

	public String getWinds() {
		return winds;
	}

	private String getVisibility() {
		return this.visibility;
	}

	public String getConditions() {
		return conditions;
	}

	public List<String> getClouds() {
		return clouds;
	}

	public Object getTemperature() {
		return temperature;
	}

	public String getDewpoint() {
		return dewPoint;
	}

	private String getAltimeter() {
		return altimeter;
	}

	private String getRemarks() {
		return this.remarks;
	}

	public static void main(String[] args) {
		MetarApp app = new MetarApp();

		try {
			
			app.retrieveMetarFor("CYOW");
			
		} catch (Exception e) {
			System.out.println(e.getMessage() + "\n\n");
			e.printStackTrace();
			return;
		}

		System.out.println("METAR: " + app.getRawMetar());
		System.out.println("Station: " + app.getStation());
		System.out.println("Report Time: " + app.getReportTime());
		System.out.println("Winds: " + app.getWinds());
		System.out.println("Visibility: " + app.getVisibility());
		System.out.println("Conditions: " + app.getConditions());
		System.out.println("Clouds: " + app.getClouds().toString());
		System.out.println("Temperature: " + app.getTemperature());
		System.out.println("Dewpoint: " + app.getDewpoint());
		System.out.println("Altimeter: " + app.getAltimeter());
		System.out.println("Remarks: " + app.getRemarks());
	}
}
