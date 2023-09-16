import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReadingSetTests
{

    @Test
    public void testConstructor()
    {
        ReadingSet rs = new ReadingSet('C', new int[]{4, 5, 6});
        assertEquals('C',rs.getTimeSlotID());
        assertEquals(4, rs.getData(0));
        assertEquals(5, rs.getData(1));
        assertEquals(6, rs.getData(2));
    }
}
