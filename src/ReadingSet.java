import java.util.Arrays;

class ReadingSet
{
    private static final int NUMBER_OF_READINGS = 3;
    private char timeSlotID;
    private int[] data;

    public ReadingSet(char timeSlotID, int[] readings)
    {
        this.timeSlotID = timeSlotID;
        this.data = readings;
    }

    @Override
    public String toString()
    {
        return "ReadingSet{" +
                "timeSlotID='" + timeSlotID + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public int getData(int index)
    {
        return data[index];
    }

    public int[] getData()
    {
        return data;
    }

    public char getTimeSlotID()
    {
        return timeSlotID;
    }
}