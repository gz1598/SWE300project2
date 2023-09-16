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

        // At this point, your logger has been set up and your datafile is ready to read
    }


    /**
     * Get the next valid entry from the file
     *
     * @return the ReadingSet for the next valid entry
     */
    public ReadingSet getNext() throws NoMoreData
    {
        return null;
    }

    /**
     * Return the min legal value for a given sensor
     *
     * @param index the offset of the sensor in the data section of each record
     * @return the min legal value
     */
    public int getMin(int index)
    {
        return 0;
    }

    /**
     * Return the max legal value for a given sensor
     *
     * @param index the offset of the sensor in the data section of each record
     * @return the max legal value
     */
    public int getMax(int index)
    {
        return 0;
    }

    /**
     * Close this reader.  Will close the log file
     */
    public void close()
    {
        LogManager.getLogManager().reset();
    }
	
	static class NoMoreData extends Exception
    {

    }
}
