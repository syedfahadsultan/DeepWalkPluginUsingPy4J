package example;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import java.util.*;

public interface EmbeddingsInterface {
    // public HashMap<String, String> getEmbeddings(ArrayList<String> relationships);
    public String getEmbeddings(ArrayList<String> relationships);
}
