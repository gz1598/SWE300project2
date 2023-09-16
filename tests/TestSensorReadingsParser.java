import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.Assert.*;

public class TestSensorReadingsParser
{

    @Test
    public void oneGoodRecord() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/OneRecord.txt");
        assertEquals(0, p.getMin(0));
        assertEquals(1, p.getMin(1));
        assertEquals(34, p.getMin(2));
        assertEquals(23, p.getMax(0));
        assertEquals(42, p.getMax(1));
        assertEquals(56, p.getMax(2));
        ReadingSet data = p.getNext();
        assertEquals('A', data.getTimeSlotID());
        assertEquals(1, data.getData(0));
        assertEquals(2, data.getData(1));
        assertEquals(35, data.getData(2));
    }

    @Test
    public void findsEndOfFile() throws SensorReadingsParser.NoMoreData, IOException
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/OneRecord.txt");

        assertNotNull(p.getNext());
        checkForEOF(p);

    }

    private static void checkForEOF(SensorReadingsParser p)
    {
        boolean foundEOF = false;
        try
        {
            p.getNext();
        }
        catch (SensorReadingsParser.NoMoreData e)
        {
            foundEOF = true;
        }
        assertTrue(foundEOF);
    }

    @Test
    public void outOfRangeHigh()
            throws IOException, ParserConfigurationException, SAXException,
            SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/OutOfRangeHigh.txt");
        ReadingSet r = p.getNext();
        for (int i = 0; i < SensorReadingsParser.NUMBER_OF_SENSORS; i++)
        {
            assertEquals(p.getMax(i), r.getData(i));
        }
        p.close();

        BasicLogParser lp = new BasicLogParser("TestFiles/OutOfRangeHigh.txt.log");
        assertEquals(3, lp.getNumberOfRecords());
        for (int i = 0; i < SensorReadingsParser.NUMBER_OF_SENSORS; i++)
        {
            assertEquals("INFO", lp.getRecordLevel(i));
        }
    }

    @Test
    public void outOfRangeLow() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/OutOfRangeLow.txt");
        ReadingSet r = p.getNext();
        for (int i = 0; i < SensorReadingsParser.NUMBER_OF_SENSORS; i++)
        {
            assertEquals(p.getMin(i), r.getData(i));
        }
        p.close();

        verifyLogSequence("TestFiles/OutOfRangeLow.txt.log",
                new String[]{"INFO", "INFO", "INFO"});
    }

    @Test
    public void outOfRangeVeryHigh() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/OutOfRangeVeryHigh.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        checkForEOF(p);
        p.close();

        verifyLogSequence("TestFiles/OutOfRangeVeryHigh.txt.log",
                new String[]{"SEVERE", "SEVERE", "SEVERE"});
    }

    private void verifyLogSequence(String logFileTitle, String[] logRecords)
    {
        try
        {
            BasicLogParser lp = new BasicLogParser(logFileTitle);
            assertEquals(logRecords.length, lp.getNumberOfRecords());
            for (int i = 0; i < logRecords.length; i++)
            {
                assertEquals(logRecords[i], lp.getRecordLevel(i));
            }
        }
        catch (Exception e)
        {
            fail("Exception while parsing log file " + e.getLocalizedMessage());
        }
    }

    @Test
    public void recordMissingData() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/BadRecordMissingColumn.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        p.close();

        verifyLogSequence("TestFiles/BadRecordMissingColumn.txt.log",
                new String[]{"SEVERE"});
    }

    @Test
    public void recordTooMuchData() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/BadRecordTooManyColumns.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        p.close();

        verifyLogSequence("TestFiles/BadRecordTooManyColumns.txt.log",
                new String[]{"SEVERE"});
    }

    @Test
    public void recordBadSensorID() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/BadRecordInvalidSensorID.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        p.close();

        verifyLogSequence("TestFiles/BadRecordInvalidSensorID.txt.log",
                new String[]{"SEVERE", "SEVERE"});
    }

    @Test
    public void badRecordlast() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/BadRecordLast.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        checkForEOF(p);
        p.close();

        verifyLogSequence("TestFiles/BadRecordLast.txt.log", new String[]{"SEVERE"});
    }

    @Test
    public void recordBadDataFormat() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/BadRecordInvalidDataFormat.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        checkForEOF(p);
        p.close();

        verifyLogSequence("TestFiles/BadRecordInvalidDataFormat.txt.log",
                new String[]{"SEVERE", "SEVERE", "SEVERE"});
    }

    @Test
    public void missingRecord() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/MissingRecord.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        ReadingSet r2 = p.getNext();
        assertEquals('C', r2.getTimeSlotID());
        for (int i = 0; i < SensorReadingsParser.NUMBER_OF_SENSORS; i++)
        {
            assertEquals(r.getData(i), r2.getData(i));
        }
        r = p.getNext();
        assertEquals('D', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('E', r.getTimeSlotID());
        p.close();
        verifyLogSequence("TestFiles/MissingRecord.txt.log", new String[]{"INFO"});
    }

    @Test
    public void missingManyRecords() throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p =
                new SensorReadingsParser("TestFiles/MissingManyRecords.txt");
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('M', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('N', r.getTimeSlotID());
        p.close();

        verifyLogSequence("TestFiles/MissingManyRecords.txt.log", new String[]{"SEVERE"});
    }

    @Test
    public void timeSlotIDSCanWrapWithoutError()
            throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/TimeSlotIDWrap.txt");
        for (int i = 0; i < SensorReadingsParser.READINGS_PER_GROUP; i++)
        {
            p.getNext();
        }
        ReadingSet r = p.getNext();
        p.close();

        assertEquals('A', r.getTimeSlotID());
        verifyLogSequence("TestFiles/TimeSlotIDWrap.txt.log", new String[]{});
    }

    @Test
    public void timeSlotIDSCanWrapMissingA()
            throws IOException, SensorReadingsParser.NoMoreData
    {
        SensorReadingsParser p = new SensorReadingsParser(
                "TestFiles" + "/TimeSlotIDWrapMissingOne" + ".txt");
        for (int i = 0; i < SensorReadingsParser.READINGS_PER_GROUP; i++)
        {
            p.getNext();
        }
        ReadingSet r = p.getNext();
        assertEquals('A', r.getTimeSlotID());
        r = p.getNext();
        assertEquals('B', r.getTimeSlotID());
        checkForEOF(p);
        p.close();


        verifyLogSequence("TestFiles/TimeSlotIDWrapMissingOne.txt.log",
                new String[]{"INFO"});
    }

    @Test
    public void matchingSetsGetLogged()
            throws IOException
    {
        SensorReadingsParser p = new SensorReadingsParser("TestFiles/MatchingData.txt");
        boolean EOF = false;
        while (!EOF)
        {
            try
            {
                ReadingSet r = p.getNext();
            }
            catch (SensorReadingsParser.NoMoreData e)
            {
                EOF = true;
            }
        }
        p.close();

        verifyLogSequence("TestFiles/MatchingData.txt.log", new String[]{"INFO",
                "INFO","INFO","INFO"});
    }
}
