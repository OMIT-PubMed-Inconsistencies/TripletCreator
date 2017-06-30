import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TripleCreator {

//

    String baseMIR="\\(?(mi|MI)(r|R)\\)?-?(\\d)+([a-zA-Z]|-?(\\d)*HG)?";
    OMITconnector oc=OMITconnector.getInstance();
    HashMap<String,Node> meshGazetteerList=oc.getTreeRootedAt("&obo;OMIT_0000110").getGazetteerList();
    HashMap<String,Node> rootNodes=new HashMap<String,Node>();
    StanfordVerbCruncher svc=StanfordVerbCruncher.getInstance();

    Pattern[] omitCandidates=new Pattern[]{
             Pattern.compile(baseMIR)   // (mir)-125b , (miR)-125b , miR-25, mir-125b
        ,    Pattern.compile(baseMIR+"-((\\d)+|3p|5p|(\\d)+-(3p|5p))") //miR-125b-5p miR-103b-1 miR-103a-2-5p
        ,    Pattern.compile("[A-Z][A-Z]+(\\d)+") //IRF4 BECN1 MCL1
        ,    Pattern.compile("[A-Z][a-z]+-(\\d)+") //Blimp-1
    };

    Matcher m;
    ArrayList<String> pubMedIds=new ArrayList<String>();
    ArrayList<String> missingFilesList=new ArrayList<String>();
    static boolean reDo=false;

    public static void main(String[] args) {
        if(args.length!=0 && args[0].equalsIgnoreCase("true")) {
            reDo=true;
        }
        TripleCreator tc = new TripleCreator();
       // Triple t1=new Triple("S1",new Node("a","A",null),"R",new Node("b","B",null),0.5f,0);
      //  Triple t2=new Triple("S2",new Node("a","A",null),"R",new Node("b","B",null),0.1f,1);
      //  System.out.println(t1.compareMerge(t2));
      //  System.out.println(t1);
    }

    public TripleCreator() {

        readList();

        for(int i=0;i<pubMedIds.size();i++) {
            String pubMedid=pubMedIds.get(i);
            System.out.println((i+1)+" out of "+pubMedIds.size()+ " is "+  pubMedid);
            try {
                ArrayList<ArrayList<String>> data= fileReader("../output/03_Ollie/"+pubMedid);
                svc.loadStanfordXML(pubMedid);
                ArrayList<Triple> triples=CreateTriples(data);
                writeToFile(pubMedid,triples);
            } catch (Exception e) {
                missingFilesList.add(pubMedid);
            }
        }

        writeList();
    }

    private void writeList(){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("../output/missing.txt", "UTF-8");
            for (int i = 0; i <missingFilesList.size() ; i++) {
                writer.println(missingFilesList.get(i));
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String pubMedid, ArrayList<Triple> triples) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("../output/04_TripleCreator/"+pubMedid+".txt", "UTF-8");
            for (int i = 0; i <triples.size() ; i++) {
                writer.println(triples.get(i));
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Triple> CreateTriples(ArrayList<ArrayList<String>> data) {
        ArrayList<Triple> triples=new ArrayList<Triple>();

        for (int i = 0; i <data.size() ; i++) {
            ArrayList<String> sentenceData=data.get(i);
            if(sentenceData.size()>0) {
                String sentence = sentenceData.get(0);
                for (int j = 1; j < sentenceData.size(); j++) {
                    try {
                        ArrayList<Triple> tr = CreateTriplet(sentence, sentenceData.get(j),i); //No need to have stanford id as i+1 because I am not using thier IDs that start from 1, I am using my own ids starting from 0
                        tr=simplyfyTriples(tr);    //Does the simplification within a sentence
                        tr=simplyfyAncestorTriples(tr); //does the simplification of ancestors within a sentence

                        triples.addAll(tr);

                    }
                    catch(Exception e){
                        //No extractions found
                    }

                }
            }
        }

        triples=simplyfyTriples(triples); //Does the simplification within the whole abstract

        return triples;

    }

    private ArrayList<Triple> simplyfyAncestorTriples(ArrayList<Triple> tr) {
        if (tr.isEmpty()) {
            return tr;
        }
        ArrayList<Triple> result=new ArrayList<Triple>();


        //Do left to Right sweep
        boolean[] merged=new boolean[tr.size()];
        merged[0]=false;

        for (int i = 0; i <tr.size() ; i++) {
            if(!merged[i]) {
                for (int j = i + 1; j < tr.size(); j++) {
                    if(!merged[j]) {
                        merged[j] = tr.get(i).oneSideAncestor(tr.get(j));

                        if(merged[j]){
                            tr.get(i).ancestorMerge(tr.get(j));
                            //System.out.println(tr.get(i).toString()+" is a decendant of "+tr.get(j).toString());
                        }

                    }
                }
                result.add(tr.get(i));
            }
        }

        tr=new ArrayList<Triple>(result);
        result=new ArrayList<Triple>();


        //Do Right to Left sweep
        merged=new boolean[tr.size()];
        merged[merged.length-1]=false;

        for (int i = tr.size()-1; i >=0 ; i--) {
            if(!merged[i]) {
                for (int j = i - 1; j >=0; j--) {
                    if(!merged[j]) {
                        merged[j] = tr.get(i).oneSideAncestor(tr.get(j));

                        if(merged[j]){
                            tr.get(i).ancestorMerge(tr.get(j));
                           // System.out.println(tr.get(i).toString()+" is a decendant of "+tr.get(j).toString());
                        }

                    }
                }
                result.add(tr.get(i));
            }
        }



        return result;
    }
    /**
     * Simplyfies the repeated Triples
     * @param tr
     * @return
     */
    private ArrayList<Triple> simplyfyTriples(ArrayList<Triple> tr){
        if(tr.isEmpty()){
            return tr;
        }

        ArrayList<Triple> result=new ArrayList<Triple>();

        boolean[] merged=new boolean[tr.size()];
        merged[0]=false;

        for (int i = 0; i <tr.size() ; i++) {
            if(!merged[i]) {
                for (int j = i + 1; j < tr.size(); j++) {
                    if(!merged[j]) {
                        merged[j] = tr.get(i).compareMerge(tr.get(j));
                    }
                }
                result.add(tr.get(i));
            }
        }


        return result;
    }


    private ArrayList<Triple> CreateTriplet(String sentence, String tripletData,int StanfordSentID){
        ArrayList<Triple> tr=new  ArrayList<Triple>();

        String[] parts=tripletData.split(":");
        parts=trimArray(parts);
        float confidance=Float.parseFloat(parts[0]);
        String[] tripParts=parts[1].substring(1,parts[1].length()-1).split(";");
        tripParts=trimArray(tripParts);



        ArrayList<Node> subject=testString(tripParts[0]);
        if(!subject.isEmpty()) {
            ArrayList<Node> object = testString(tripParts[2]);
            if(!object.isEmpty()) {

                String verb=svc.crunch(tripParts[1],StanfordSentID);

                for (int i = 0; i <subject.size() ; i++) {
                    for (int j = 0; j <object.size() ; j++) {
                        if(!subject.get(i).id.equalsIgnoreCase(object.get(j).id)) { //Prevents self looping triplets
                            tr.add(new Triple(sentence, subject.get(i), verb, object.get(j), confidance,StanfordSentID));
                        }

                    }
                }
            }
        }




        return tr;

    }

    private String[] trimArray(String[] arr){
        for (int i = 0; i < arr.length ; i++) {
            arr[i]=arr[i].trim();
        }
        return arr;
    }

    private ArrayList<Node> testString(String s){
        if(s.contains(" ")){
            ArrayList<Node> nodes=new ArrayList<Node>();

            Iterator<String> itr=meshGazetteerList.keySet().iterator();
            String simpleS,simpleMesh;
            while(itr.hasNext()){
                String meshTerm=itr.next();

                simpleS=s.toLowerCase();
                simpleMesh=meshTerm.toLowerCase();

                if(simpleS.contains(simpleMesh)){
                    if(simpleS.contains(" "+simpleMesh)) { //The meshterm is at anywhere but the begining of the sentence
                        nodes.add(meshGazetteerList.get(meshTerm));
                    }
                    else if (simpleS.startsWith(simpleMesh+" ")||simpleS.startsWith(simpleMesh+".")||simpleS.startsWith(simpleMesh+",")){
                        nodes.add(meshGazetteerList.get(meshTerm));
                    }
                }

            }

            String[] parts=s.split(" ");
           // System.out.println(testPatterns(parts[0]));

            for (int i = 0; i <parts.length ; i++) {
                nodes.addAll(testPatterns((parts[i])));
            }


            return nodes;
        }
        else{
            return testPatterns(s);
        }
    }


    private ArrayList<Node> testPatterns(String s){
        ArrayList<Node> nodes=new ArrayList<Node>();

        //Sometimes two things are given like miR-19a/b
        if(s.contains("/")) {
            String[] parts = s.split("/");
            s = parts[0];
            parts[1] = parts[0].substring(0, parts[0].length() - 1) + parts[1];
            nodes.addAll(testPatterns(parts[1]));
        }

        Node n=null;



        if(match(omitCandidates[1],s)){  //if the second one is matched then either a specific human one rooted at human_miRNA or other rooted at miRNA_target_gene

            s=s.toUpperCase(); //All are in uppercase.
            s=s.replace("(","");
            s=s.replace(")","");
            n=OMITcheck("&obo;NCRO_0000810","hsa-"+s); //do a search in human_miRNA ( "&obo;NCRO_0000810") first, if it does not work, look in miRNA_target_gene

            s=s.replace("MIR-","MIR"); //only the first one should be gone
            if(n==null){
                n=OMITcheck("&obo;NCRO_0000025",s);
            }

        }
        else if(match(omitCandidates[0],s) || match(omitCandidates[2],s)){ //if first one or third one is matched then rooted at miRNA_target_gene

           s=s.toUpperCase(); //All are in uppercase.
           s=s.replace("(","");
           s=s.replace(")","");
           s=s.replace("MIR-","MIR"); //only the first one should be gone
           // System.out.println(s);
           n=OMITcheck("&obo;NCRO_0000025",s);  //do a search in miRNA_target_gene "&obo;NCRO_0000025"
        }
        else{
            n=OMITcheck("&obo;OMIT_0000110",s);  //do a search in MESH_term  "&obo;OMIT_0000110"
        }

        nodes.add(n);

        nodes.removeAll(Collections.singleton(null));


        return nodes;
    }

    private Node OMITcheck(String root,String name){
        Node r=rootNodes.get(root);
        if(r==null){
            r=oc.getTreeRootedAt(root);
            rootNodes.put(root,r);
        }

        Node n=r.findNode(name);

        return (n);
    }


    private void RunPatternTest() {
        System.out.println(match(omitCandidates[0],"(mir)-125b"));
        System.out.println(match(omitCandidates[0],"(miR)-125b"));
        System.out.println(match(omitCandidates[0],"miR-25"));
        System.out.println(match(omitCandidates[0],"mir-125b"));
        System.out.println(match(omitCandidates[0],"mir-3945HG"));
        System.out.println(match(omitCandidates[0],"MIR3945"));
        System.out.println(match(omitCandidates[0],"MIR194-2HG"));
        System.out.println(match(omitCandidates[1],"miR-125b-5p"));
        System.out.println(match(omitCandidates[1],"miR-103b-1"));
        System.out.println(match(omitCandidates[1],"miR-103a-2-5p"));
        System.out.println(match(omitCandidates[1],"miR-1-1"));
        System.out.println(match(omitCandidates[2],"IRF4"));
        System.out.println(match(omitCandidates[2],"BECN1"));
        System.out.println(match(omitCandidates[2],"MCL1"));
        System.out.println(match(omitCandidates[3],"Blimp-1"));


        oc.print(testString("mir-3945HG"));
        oc.print(testString("mir-3913-1"));
        oc.print(testString("IRF4"));
        oc.print(testString("clinical study"));
        oc.print(testString("macrophages"));
        oc.print(testString("miR-19b"));
        oc.print(testString("miR-19a/b"));
        oc.print(testString("the anti-MM activity of miR-125b-5p"));
        oc.print(testString("inhibition of IRF4"));
        oc.print(testString("significant anti-tumor activity and prolonged survival"));
        oc.print(testString("vivo intra-tumor or systemic delivery of formulated miR-125b-5p mimics"));
        oc.print(testString("downregulation of c-Myc , caspase-10 and cFlip , relevant IRF4-downstream effectors"));
        oc.print(testString("miR-125b"));
        oc.print(testString("TC2/3 molecular MM subgroups"));
        oc.print(testString("expression of IRF4 mRNA"));
        oc.print(testString("microRNA"));
    }


    private boolean match(Pattern p,String s){
        m = p.matcher(s);
        return m.matches();
    }

    private ArrayList<ArrayList<String>> fileReader(String path) throws Exception {
        ArrayList<ArrayList<String>> data=new ArrayList<ArrayList<String>>();
        ArrayList<String> sentence=new ArrayList<String>();

            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            String s;
            while ((s = br.readLine()) != null) {
                if(s.isEmpty()) {
                    data.add(sentence);
                    sentence=new ArrayList<String>();
                }
                else{
                    sentence.add(s);
                }
            }
            data.add(sentence);
            fr.close();

        return data;

    }

    private void readList(){
        if(reDo){
            File folder = new File("../output/02_Stanford");
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                //System.out.println();
                pubMedIds.add(listOfFiles[i].getName().split("\\.")[0]);
            }
        }
        else {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader("../pubmed-list.txt");
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    //  System.out.println(line);
                    pubMedIds.add(line);
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
