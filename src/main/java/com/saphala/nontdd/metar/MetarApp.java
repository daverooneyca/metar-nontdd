package com.saphala.nontdd.metar;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class MetarApp {
   private String apiKey = null;
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

   public Map<String, Object> retrieveMetarFor(String station) throws Exception {
      try {
         // Retrieve the METAR from CheckWX
         HttpResponse<MetarResult> metarResponse = Unirest.get("https://api.checkwx.com/metar/{station}/")
               .header("accept", "application/json").header("X-API-Key", this.apiKey).routeParam("station", station)
               .asObject(MetarResult.class);

         MetarResult result = metarResponse.getBody();

         String rawMetar = result.getData().get(0);
         
         System.out.println(rawMetar);

         // Check for any errors
         if (rawMetar.toLowerCase().contains("invalid station")) {
            throw new Exception("No results found for station: " + station);
         }

         if (rawMetar.toLowerCase().contains("currently unavailable")) {
            throw new Exception("METAR currently unavailable for station: " + station);
         }

         // Parse the METAR string
         String[] metar = rawMetar.split(" ");

         Map<String, Object> metarMap = new HashMap<String, Object>();

         if (metar.length == 0) {
            return metarMap;
         }
         
         List<String> metarList = Arrays.asList(metar);

         metarMap.put("station", metarList.get(0));
         metarMap.put("reportTime", metarList.get(1));

         // Get the winds
         this.windsStartIndex = 2;

         if (Character.isAlphabetic(metarList.get(2).charAt(0))) {
            windsStartIndex++;
         }
         
         int startPosition = windsStartIndex;
         int endPosition = windsStartIndex + 1;
         
         List<String> winds = metarList.subList(startPosition, metarList.size());
         
         boolean variableWindsFound = false;
         
         for (String entry : winds) {
            if (entry.contains("V") && Character.isDigit(entry.charAt(0))) {
               variableWindsFound = true;
               break;
            }
         
            endPosition++;
         }
         
         if (variableWindsFound) {
            this.windsEndIndex = endPosition;
         } else {
            this.windsEndIndex = this.windsStartIndex + 1;
         }

         metarMap.put("winds", metarList.subList(windsStartIndex, windsEndIndex).toString());
         
         // Visibility
         startPosition = windsStartIndex + 1;
         endPosition = startPosition + 1;
         
         for (String entry : metarList.subList(startPosition, metarList.size())) {
            if (entry.endsWith("SM")) {
               break;
            }
            endPosition++;
         }
                  
         List<String> visibilityList = metarList.subList(startPosition, endPosition);
         
         String rawVisibility = "";
         
         for (String entry : visibilityList) {
            rawVisibility = rawVisibility + entry + " ";
         }
         
         this.visibilityEndIndex = endPosition;
         
         metarMap.put("visibility", rawVisibility.trim());
         
         // Clouds
         List<String> cloudPrefixes = Arrays.asList("VV", "SKC", "CLR", "FEW", "SCT", "BKN", "OVC");
         
         startPosition = this.visibilityEndIndex;
         endPosition = 0;
         boolean found = false;
         
         for (String entry : metarList) {
            for (String prefix : cloudPrefixes) {
               if (entry.startsWith(prefix)) {
                  found = true;
                  break;
               }
            }
         
            if (found) {
               break;
            }
         
            endPosition++;
         }
         
         if (endPosition < startPosition) {
            endPosition = startPosition;
         }
         
         List<String> conditionsList = metarList.subList(startPosition, endPosition);
         
         metarMap.put("conditions", conditionsList.toString());
         
         startPosition = 0;
         
         found = false;
         
         for (String entry : metarList) {
            for (String prefix : cloudPrefixes) {
               if (entry.startsWith(prefix)) {
                  found = true;
                  break;
               }
            }
         
            if (found) {
               break;
            }
         
            startPosition++;
         }
         
         endPosition = startPosition;
         
         for (String entry : metarList.subList(startPosition, metarList.size())) {
            char firstChar = entry.charAt(0);
            if (Character.isDigit(firstChar)) {
               break;
            }
            endPosition++;
         }
         
         this.cloudsEndIndex = endPosition;
         
         metarMap.put("clouds", metarList.subList(startPosition, endPosition));
         
         String tempAndDewpoint = metarList.get(this.cloudsEndIndex);
         
         metarMap.put("temperature", tempAndDewpoint.split("/")[0]);
         
         metarMap.put("dewPoint", tempAndDewpoint.split("/")[1]);
         
         metarMap.put("altimeter", metarList.get(cloudsEndIndex + 1));
         
         metarMap.put("remarks", metarList.subList(cloudsEndIndex + 2, metarList.size()).toString());

         return metarMap;
         
      } catch (UnirestException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } finally {
         Unirest.shutdown();
      }
      return null;
   }

   public int getWindsIndex() {
      return windsStartIndex;
   }

   public int getVisibilityEndIndex() {
      return visibilityEndIndex;
   }

   public static void main(String[] args) {
      MetarApp app = new MetarApp();

      try {

         Map<String, Object> metar = app.retrieveMetarFor("CYOW");

         System.out.println("Station: " + metar.get("station"));
         System.out.println("Report Time: " + metar.get("reportTime"));
         System.out.println("Winds: " + metar.get("winds"));
         System.out.println("Visibility: " + metar.get("visibility"));
         System.out.println("Conditions: " + metar.get("conditions"));
         System.out.println("Clouds: " + metar.get("clouds").toString());
         System.out.println("Temperature: " + metar.get("temperature"));
         System.out.println("Dewpoint: " + metar.get("dewPoint"));
         System.out.println("Altimeter: " + metar.get("altimeter"));
         System.out.println("Remarks: " + metar.get("remarks"));
         
      } catch (Exception e) {
         System.out.println(e.getMessage() + "\n\n");
         e.printStackTrace();
      }
   }
}
