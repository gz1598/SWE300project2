import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * This class parses a log file only enough to be able to determine the types of log
 * entries that exist in the file.
 */
public class BasicLogParser
{
    private final NodeList records;
    private final Document document;
    private final Element root;

    /**
     *
     * @param logFileTitle the name of the file we should parse
     * @throws ParserConfigurationException If you see this, make sure you "closed"
     * your SensorReadingsParser
     * @throws IOException shouldn't
     * @throws SAXException If you see this, make sure you "closed"
     * your SensorReadingsParser
     */
    public BasicLogParser(String logFileTitle) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(new File(logFileTitle));
        document.getDocumentElement().normalize();

        root = document.getDocumentElement();
        records = document.getElementsByTagName("record");
    }

    /**
     *
     * @return the number of log entries in the file
     */
    public int getNumberOfRecords()
    {
        return records.getLength();
    }

    /**
     * The the log level of the specified record
     * @param index the record number we should look at
     * @return the log level of that record (null if there is no such record)
     */
    public String getRecordLevel(int index)
    {
        Node node = records.item(index);
        NodeList children = node.getChildNodes();
        for(int i=0;i<children.getLength();i++)
        {
            Node child = children.item(i);
            if (child.getNodeName().equals("level"))
            {
                String nodeValue = child.getChildNodes().item(0).getNodeValue();
                return nodeValue;
            }
        }
        return null;
    }
}
