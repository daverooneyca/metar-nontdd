# metar-nontdd - Parsing and Decoding METAR Data Exercise, done without TDD

This is a complementary exercise to [https://github.com/daverooneyca/tdd-metar](tdd-metar), except that in this case the 
code was written without using a TDD approach, nor was it written initially with any unit tests.

The exercise is based on parsing and decoding aviation weather METAR data in Java. A typical METAR looks like this:

     CYOW 081900Z 33007KT 15SM -RA SCT030TCU BKN100 BKN180 24\/22 A2984 RMK TCU4AC2AC1 SLP107 DENSITY ALT 1600FT

This is read as:
 - Station: Ottawa Macdonald/Cartier Airport, ICAO code 'CYOW'
 - Reporting Time: the day of the month 08, time in 'Zulu' or UTC 19:00
 - Wind Velocity: 330 degrees at 7 knots
 - Visibility: 15 statute miles
 - Conditions: -RA, rain
 - Clouds:
   - Scattered, 3000 feet AGL (above ground level), towering cumulus (TCU)
   - Broken, 10000 feet AGL
   - Broken, 18000 feet AGL
 - Temperature & Dewpoint: 24C and 22C, respectively
 - Altimeter Setting: 29.84 inches of mercury
 - Remarks: (not implementing them in this exercise)

The definitions and specifications used for the code are found in [MANOBS Manual of Surface Weather Observations: aviation routine weather report from Environment Canada](https://www.canada.ca/en/environment-climate-change/services/weather-manuals-documentation/manobs-surface-observations/aviation-routine-report.html).

The `master` branch contains the final version of the Java code used in the exercise. Interim steps are contained in the various branches.

The overall goals of the exercise were:
 - to illustrate the differences in code built using a non-TDD approach
 - to highlight that it's actually slower to code this way than using TDD once you have the hang of it
