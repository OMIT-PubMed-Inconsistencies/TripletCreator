import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Nisansa on 11/7/2016.
 */
public class StanfordVerbCruncher {

    private static StanfordVerbCruncher svc=new  StanfordVerbCruncher();
    private ArrayList<Element> nodes=new ArrayList<Element>();

    public  static StanfordVerbCruncher getInstance(){
        return svc;
    }


    public String crunch(String verbPart, int stanfordSentID) {

        Element sentence=nodes.get(stanfordSentID); //This is the relevant sentence

        //System.out.println(verbPart);

        if(!verbPart.contains(" ")){ //if it is a single word, just find the lemma of the word

            NodeList wList=sentence.getElementsByTagName("word"); //word list
            NodeList lList=sentence.getElementsByTagName("lemma");  //lemma list

            for (int i = 0; i <wList.getLength() ; i++) { //Iterating among words
                org.w3c.dom.Node nNode = wList.item(i);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    if (nNode.getTextContent().equalsIgnoreCase(verbPart)) {
                        verbPart=lList.item(i).getTextContent();

                    }
                }
            }
        }
        else{
                //TODO what happens when it is not a single word

        }

        return verbPart;
    }

    public void loadStanfordXML(String pubMedid) {
        nodes=new ArrayList<Element>();
        File fXmlFile = new File("../output/02_Stanford/"+pubMedid+".out");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder =dbFactory.newDocumentBuilder();
            Document doc =dBuilder.parse(fXmlFile);
            NodeList nList= doc.getElementsByTagName("sentence");
            Element eElement=null;
            String idString="";
            for (int i = 0; i <nList.getLength() ; i++) {
                org.w3c.dom.Node nNode = nList.item(i);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    eElement = (Element) nNode;
                    idString=eElement.getAttribute("id");
                    if(idString.length()>0) {
                        int id = Integer.parseInt(eElement.getAttribute("id"));
                        nodes.add(eElement);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
