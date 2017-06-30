import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Nisansa on 10/17/2016.
 */
public class Triple {
    Node w1;
    String c;
    Node w2;
    float confidance;
    ArrayList<String> sentences=new ArrayList<String>();
    ArrayList<Integer> stanfordId=new ArrayList<Integer>();


    public Triple(String sentence, Node w1,String c, Node w2, float confidance,int stanfordId) {
        this.w1 = w1;
        this.c = c;
        this.sentences.add(sentence);
        this.w2 = w2;
        this.confidance = confidance;
        this.stanfordId.add(stanfordId);
    }

    public String toString(){
        StringBuilder sb=new StringBuilder();
        for (int i = 0; i <stanfordId.size() ; i++) {
            sb.append(confidance);
            sb.append(" (");
            sb.append(w1.name);
            sb.append(" ; ");
            sb.append(c);
            sb.append(" ; ");
            sb.append(w2.name);
            sb.append(") ");
            sb.append(stanfordId.get(i));
            if(i<stanfordId.size()-1){
                sb.append("\n");
            }
        }

        //return confidance+" ("+w1.name+" ; "+c+" ; "+w2.name+") "+stanfordId;
        return sb.toString();
    }

    public boolean compare(Triple t){
        return ((c.equalsIgnoreCase(t.c))&&(w1.id.equalsIgnoreCase(t.w1.id))&&(w2.id.equalsIgnoreCase(t.w2.id)));
    }

    /**
     * Sees if one side of the given triplet is the same as this while the other is an ancestor
     * @param t
     * @return
     */
    public boolean oneSideAncestor(Triple t){
        if(c.equalsIgnoreCase(t.c)) {
            if(w1.id.equalsIgnoreCase(t.w1.id)){ //Left is the same see if right is an ancestor
                return w2.isAncestor(t.w2);
            }
            else if(w2.id.equalsIgnoreCase(t.w2.id)){ //Right is the same see if left is an ancestor
                return w1.isAncestor(t.w1);
            }

        }
        return false;
    }

    public void ancestorMerge(Triple t){
        confidance=(confidance+t.confidance)/2;
    }

    public boolean compareMerge(Triple t){
        if(compare(t)){
            if(sentences.get(0).equalsIgnoreCase(t.sentences.get(0)) && sentences.size()==1 && t.sentences.size()==1){
                confidance=(confidance+t.confidance)/2;
            }
            else{
                confidance=confidance*sentences.size()+t.confidance*t.sentences.size();

                HashSet<String> senten=new HashSet<String>();
                senten.addAll(sentences);
                senten.addAll(t.sentences);

                HashSet<Integer> ids=new HashSet<Integer>();
                ids.addAll(stanfordId);
                ids.addAll(t.stanfordId);

                confidance/=senten.size();
                sentences=new ArrayList<String>(senten);
                stanfordId=new ArrayList<Integer>(ids);
            }

            return true;
        }

        return false;
    }
}

