import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class parses a sensor log file.  In addition to returning the ReadingSets that it finds, it logs
 * any errors it saw in the file.  Those logs will be a file whose title and path match the file being
 * read with ".log" added at the end of the file title.
 */
public class SensorReadingsParser
{
    static final int NUMBER_OF_SENSORS = 3;
    static final char FIRST_TIME_SLOT_ID = 'A';
    static final int READINGS_PER_GROUP = 15;
    private static final Logger logger =
            Logger.getLogger(SensorReadingsParser.class.getName());
    private final Scanner dataFile;

    private static final String DELIMITING_CHAR = " ";
    private static final char TIME_SLOT_ID_MIN = 'A';
    private static final char TIME_SLOT_ID_MAX = 'O';
    private static final int TIME_SLOT_ID_POSITION = 0;
    private static final int TIME_SLOT_ID_LENGTH_EXPECTED = 1;
    private static final int NUM_DATA_ENTRIES_EXPECTED = 4;
    private static final int FIRST_SENSOR_READING_POSITION = 1;
    private static final double DATA_MAX_PERCENT_TOLERANCE = 1.5;
    private int[][] sensors = { { 0, 0 }, { 0, 0 }, { 0, 0 } };
    private char expectedTimeSlotId = FIRST_TIME_SLOT_ID;
    private int[] previousSensorData = {0, 0, 0};
    private int[] sensorReadings = {0, 0, 0};
    private char timeSlotId = FIRST_TIME_SLOT_ID;
    private boolean isMissingOnePreviousReading = false;
    private String lineInFile = "";

    /**
     * Create an object that can read our sensor data files
     *
     * @param fileTitle the title of the file this object should read
     * @throws IOException if it can't find the file or create an appropriate log file
     */
    public SensorReadingsParser(String fileTitle) throws IOException
    {
        FileHandler fileHandler = new FileHandler(fileTitle + ".log");

        //Assigning handlers to LOGGER object
        logger.addHandler(fileHandler);

        fileHandler.setLevel(Level.INFO);
        logger.setLevel(Level.ALL);

        dataFile = new Scanner(new File(fileTitle));

        int lineCount = 0;
        String lineInFile;
        String[] sensorInput;
        String delimiter = " ";
        while(lineCount < NUMBER_OF_SENSORS) {
            lineInFile = dataFile.nextLine();
            sensorInput = lineInFile.split(delimiter);
            for (int index = 0; index < 2; index++)
            {
                sensors[lineCount][index] = Integer.parseInt(sensorInput[index]);
            }
            lineCount++;
        }

        // At this point, your logger has been set up and your datafile is ready to read
    }

    /**
     * Get the next valid entry from the file
     *
     * @return the ReadingSet for the next valid entry
     */
    public ReadingSet getNext() throws NoMoreData {
        // check for eof if previous line was not skipped
        if (!isMissingOnePreviousReading) {
            if(!dataFile.hasNextLine()) {
                throw new NoMoreData();
            }
            lineInFile = dataFile.nextLine();
        } else {
            isMissingOnePreviousReading = false;
        }
        String[] parts = lineInFile.split(DELIMITING_CHAR);
        timeSlotId = parts[TIME_SLOT_ID_POSITION].charAt(0);
        boolean isValidTimeID = isTimeSlotIDValid(parts[TIME_SLOT_ID_POSITION]);

        if (isDataAmountValid(parts.length) && (doesSensorReadingContainChar(parts))
                && isValidTimeID) {
            // read in data and check for out of range
            readInSensorData(parts);
            // check if data is matching
            checkMatching(sensorReadings);

            previousSensorData = sensorReadings;
        }

        //determine the next expected Time Slot ID
        expectedTimeSlotId = calcExpectedTimeSlotId(timeSlotId);
        // return reading set with data and time slot id
        ReadingSet readingSet = new ReadingSet(timeSlotId, sensorReadings);
        return readingSet;
    }

    /**
     * Checks the bounds of the sensor readings and stores them for future use.
     *
     * @param lineOfData the line of data to be read from.
     */
    private void readInSensorData(String[] lineOfData) {
        for (int index = 0; index < NUMBER_OF_SENSORS; index++) {
            sensorReadings[index] = Integer.parseInt(lineOfData[index + 1]);
            checkSensorReadingTooLow(index);
            checkSensorReadingTooHigh(index);
            if (isSensorReadingExceedingMaxTolerance(index)) {
                lineInFile = dataFile.nextLine();
                timeSlotId = lineInFile.charAt(0);
                lineOfData = lineInFile.split(" ");
            }
        }
    }

    /**
     * Return the min legal value for a given sensor
     *
     * @param index the offset of the sensor in the data section of each record
     * @return the min legal value
     */
    protected int getMin(int index) {
        return sensors[index][0];
    }

    /**
     * Return the max legal value for a given sensor
     *
     * @param index The offset of the sensor in the data section of each record
     * @return the max legal value
     */
    protected int getMax(int index) {
        return sensors[index][1];
    }

    /**
     * Close this reader.  Will close the log file
     */
    protected void close() {
        LogManager.getLogManager().reset();
    }

    /**
     * Checks if the length of a given data set is valid.
     * If the data amount is invalid, it will be logged.
     * @param dataLength The length of the data set.
     * @return True if the data amount is valid, false if not.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private boolean isDataAmountValid(int dataLength) throws NoMoreData {
        if (dataLength < NUM_DATA_ENTRIES_EXPECTED) {
            logRecordMissingData();
            return false;
        }
        if (dataLength > NUM_DATA_ENTRIES_EXPECTED) {
            logRecordTooMuchData();
            return false;
        }
        return true;
    }

    /**
     * Sends message to logger that there is fewer data than expected.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private void logRecordMissingData() throws NoMoreData {
        logger.severe("Record is missing data");
        close();
        getNext();
    }

    /**
     * Sends message to logger that there is more data than expected.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private void logRecordTooMuchData() throws NoMoreData {
        logger.severe("Record has too much data");
        close();
        getNext();
    }

    /**
     * Checks if the current time slot ID of the entry is valid.
     * @param readTimeSlotID The most recently read time slot ID.
     * @return True if the time slot ID is valid, false if not.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private boolean isTimeSlotIDValid(String readTimeSlotID) throws NoMoreData {
        int timeSlotIDLength = readTimeSlotID.length();
        if (!isTimeSlotIDInRange()) {
            timeSlotId = expectedTimeSlotId;
            logTimeSlotIDOutOfRange();
            return false;
        }
        if (!isTimeSlotIDValidLength(timeSlotIDLength)) {
            timeSlotId = expectedTimeSlotId;
            logTimeSlotIDInvalidLength();
            return false;
        }
        if (timeSlotId != expectedTimeSlotId) {
            int distanceOff = calcNumOfPositionsOff(timeSlotId, expectedTimeSlotId);
            //Check if one line was missed.
            if (distanceOff == 1) {
                //Incorrect TimeSlotID is set to the expected ID
                isMissingOnePreviousReading = true;
                timeSlotId = expectedTimeSlotId;
                sensorReadings = previousSensorData;
                logger.info("Missing reading. Returning all values of the " +
                        "last reading with expected ID.");
                //getNext(); removed
                return false;
            } else if (distanceOff > 1) {
                logger.severe("Time Slot ID is off by more than 1 position. " +
                        "Going forward as if entry is correct.");
            }
        }
        return true;
    }

    /**
     * Checks if the current time slot ID is in the valid range.
     * @return True if the time slot ID is in the valid range, false if not.
     */
    private boolean isTimeSlotIDInRange() {
        if (TIME_SLOT_ID_MIN <= timeSlotId && timeSlotId <= TIME_SLOT_ID_MAX) {
            return true;
        }
        return false;
    }

    /**
     * Sends a message to the logger if the current time slot ID is out of
     * the valid range.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private void logTimeSlotIDOutOfRange() throws NoMoreData {
        System.out.println("Time Slot ID is out of range");
        logger.severe("Time Slot ID is out of range, replaced with expected ID");
        getNext();
    }

    /**
     * Checks if the length of a time slot ID is valid
     * @param timeSlotIDLength The length of the time slot ID
     * @return True if the time slot ID length is valid, false if not.
     */
    private boolean isTimeSlotIDValidLength(int timeSlotIDLength) {
        if (timeSlotIDLength == TIME_SLOT_ID_LENGTH_EXPECTED) {
            return true;
        }
        return false;
    }

    /**
     * Sends a message to the logger that the time slot ID length is invalid.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private void logTimeSlotIDInvalidLength() throws NoMoreData{
        System.out.println("Time Slot ID is too long");
        logger.severe("Time Slot ID is too long");
        getNext();
    }

    /**
     * Calculates the expected time slot ID of the next entry.
     * @param currId The current time slot ID.
     * @return The next expected time slot ID.
     */
    public char calcExpectedTimeSlotId(char currId) {
        if (currId == TIME_SLOT_ID_MAX) {
            return TIME_SLOT_ID_MIN;
        } else {
            int asciiOfCurrId = currId;
            asciiOfCurrId++;
            return (char) asciiOfCurrId;
        }
    }

    /**
     * Calculates the number of positions off the current time slot ID is
     * from the expected time slot ID.
     * @param asciiOfCurrId The ASCII value for the character of the
     *                      current time slot ID.
     * @param asciiOfExpectedValue The ASCII value for the character of
     *                             the current time slot ID.
     * @return The number of positions off the current time slot ID is
     * from the expected time slot ID.
     */
    public int calcNumOfPositionsOff(int asciiOfCurrId, int asciiOfExpectedValue) {
        return asciiOfCurrId - asciiOfExpectedValue;
    }

    /**
     * Checks if any sensor readings contain a character instead of an integer.
     * @param lineOfData The most recently read line of data.
     * @return True if none of the sensor readings contain a character,
     * false if any sensor readings contain a character.
     * @throws NoMoreData Throws exception if there is no more data.
     */
    private boolean doesSensorReadingContainChar(String[] lineOfData) throws NoMoreData {
        for (int sensorIndex = 0; sensorIndex < NUMBER_OF_SENSORS; sensorIndex++) {
            String currentSensorReading =
                    lineOfData[sensorIndex + FIRST_SENSOR_READING_POSITION];
            for (int readingValueIndex = 0; readingValueIndex < currentSensorReading.length();
                 readingValueIndex++) {
                if (Character.isLetter(currentSensorReading.charAt(readingValueIndex))) {
                    // reruns line with C in sensor 1 data slot
                    logger.severe("Sensor reading is not a number");
                    getNext();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if any of the sensor readings match another reading.
     * @param sensorReadings The current sensor readings.
     */
    private void checkMatching(int[] sensorReadings) {
        for (int currentIndex = 0; currentIndex < NUMBER_OF_SENSORS - 1; currentIndex++) {
            for (int checkingIndex = currentIndex + 1;
                 checkingIndex < NUMBER_OF_SENSORS - currentIndex; checkingIndex++) {
                if (sensorReadings[currentIndex] == sensorReadings[checkingIndex]) {
                    String logMessage = "Sensor " + ++currentIndex + " and " + ++checkingIndex + " are matching";
                    logger.info(logMessage);
                }
            }
        }
        /**
         if (sensorReadings[0] == sensorReadings[1]) {
         logger.info("Sensor 1 and 2 are matching");
         } else if (sensorReadings[0] == sensorReadings[2]) {
         logger.info("Sensor 1 and 3 are matching");
         } else if (sensorReadings[1] == sensorReadings[2]) {
         logger.info("Sensor 2 and 3 are matching");
         }
         */
    }


    private boolean isOutOfRangeCheck(int sensorIndex) {
        checkSensorReadingTooLow(sensorIndex);
        checkSensorReadingTooHigh(sensorIndex);
        int max = getMax(sensorIndex);
        double maxWithTolerance = max * DATA_MAX_PERCENT_TOLERANCE;
        if (sensorReadings[sensorIndex] >= maxWithTolerance && dataFile.hasNext()) {
            sensorReadings[sensorIndex] = max;
            logger.severe("Reading value is at least 150% of max value.  Setting to max value");
            return true;
        }
        return false;
    }

    /**
     * Checks if a sensor reading is below the minimum allowed.
     * @param sensorIndex The index of the desired sensor.
     */
    private void checkSensorReadingTooLow(int sensorIndex) {
        int min = getMin(sensorIndex);
        if (sensorReadings[sensorIndex] < min) {
            sensorReadings[sensorIndex] = sensors[sensorIndex][0];
            logger.info("Reading value is too low.  Setting to min value");
        }
    }

    /**
     * Checks if a sensor reading is above the maximum allowed, but below
     * 150% of the max.
     * @param sensorIndex The index of the desired sensor.
     */
    private void checkSensorReadingTooHigh(int sensorIndex) {
        int max = getMax(sensorIndex);
        double maxWithTolerance = max * DATA_MAX_PERCENT_TOLERANCE;
        if (max < sensorReadings[sensorIndex] && sensorReadings[sensorIndex] < maxWithTolerance) {
            sensorReadings[sensorIndex] = sensors[sensorIndex][1];
            logger.info("Reading value is too high.  Setting to max value");
        }
    }

    /**
     * Checks if a sensor reading is above 150% maximum threshold.
     * @param sensorIndex The index of the desired sensor.
     * @return True if the sensor reading exceeds the max given the 150% tolerance,
     * false if it does not.
     */
    private boolean isSensorReadingExceedingMaxTolerance(int sensorIndex) {
        int max = getMax(sensorIndex);
        double maxWithTolerance = max * DATA_MAX_PERCENT_TOLERANCE;
        if (sensorReadings[sensorIndex] >= maxWithTolerance && dataFile.hasNext()) {
            sensorReadings[sensorIndex] = max;
            logger.severe("Reading value is at least 150% of max value.  Setting to max value");
            return true;
        }
        return false;
    }

    /**
     * Exception for when a file is trying to be read but there is
     * no more data left to read.
     */
    static class NoMoreData extends Exception
    {
    }
}
