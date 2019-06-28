
package example;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.GraphDatabaseService;
// import org.neo4j.graphdb.GraphDatabaseAPI;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.neo4j.graphdb.Relationship;

import java.util.*;
import java.io.*;
import java.lang.InterruptedException;
import java.lang.Long;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.nio.file.Paths;
/**
 * This is an example how you can create a simple user-defined function for Neo4j.
 */
public class DeepWalk
{
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

	private Number exportData(String path) throws IOException{
		log.info("Deepwalk: Exporting graph as an edgelist csv");

        FileWriter csvWriter = new FileWriter(path+"/data/tmp.csv");

        for (Relationship relationship : db.getAllRelationships()) {
        	long startNodeId = relationship.getStartNodeId();
        	long endNodeId = relationship.getEndNodeId();

            csvWriter.append(startNodeId+" "+endNodeId);
 			csvWriter.append("\n");	
        }

        csvWriter.flush();  
		csvWriter.close();

		return db.getAllRelationships().stream().count();
    }

    // @UserFunction
    // @Description("example.exportdata_cypher() - a dumb method that just returns number of nodes")
    // public boolean exportdata_cypher() throws KernelException{
    private boolean exportdata_cypher() throws KernelException{

        try (Transaction tx = db.beginTx()) {
            db.execute("CALL apoc.export.csv.query('MATCH (a:Node)-"+
            	"[r:LINKED_TO]->(b:Node) RETURN a.id,"+
            	" b.id', '../data/results.csv', {})").close();
            tx.success();
        }

        return true;
    }

	private int learnEmbeddings(String path, String deepwalkPath) throws IOException, InterruptedException{
		log.info("Deepwalk: Calling bash command for deepwalk ");
    	String toreturn = "";
    	String s = "";
    	List<String[]> emb = null;

    	String[] bashCommand = new String[]{
            deepwalkPath,
    		"--format", "edgelist",
            "--input", path+"/data/tmp.csv", 
            "--output", path+"/data/tmp.embeddings"
    	};

	    Process process = Runtime.getRuntime().exec(bashCommand);

	    BufferedReader reader = new BufferedReader(new InputStreamReader(        
	        process.getInputStream()));                                          
	    while ((s = reader.readLine()) != null) {                                
	      log.info("Deepwalk: "+ s);
	    }  

	    BufferedReader errReader = new BufferedReader(new InputStreamReader(        
	        process.getErrorStream()));                                          
	    while ((s = errReader.readLine()) != null) {
	    	log.info("Deepwalk-ERR: "+ s);
	    }
	    int exitVal = process.waitFor();
	    return exitVal;
	}


    public boolean loadEmbeddings(String path) throws IOException, FileNotFoundException { 
    	log.info("Deepwalk: Writing Embeddings as Node Attributes");
	    
    	ResourceIterable<Node> nodes = db.getAllNodes();

	    int count = 0;
	    String file = path+"/data/tmp.embeddings";
	    BufferedReader br = new BufferedReader(new FileReader(file));
        String line = "";
        line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] content = line.split(" ");
            String[] emb = Arrays.copyOfRange(content, 1, content.length);
            Node node = db.getNodeById(Long.parseLong(content[0]));
            node.setProperty("embedding", emb);
        }

        return true;
	}


	@Procedure(name="example.deepWalk", mode=Mode.WRITE)
	@Description("Calls deepwalk from bash and writes back embeddings as node properties")
    public void deepWalk() throws IOException, FileNotFoundException, InterruptedException, KernelException{

        String deepwalkPath = System.getenv("DEEPWALK");
        String dir = System.getenv("NEO4J_HOME");
        log.info("Deepwalk - Path: "+deepwalkPath);
        log.info("Deepwalk - Dir: "+dir);

		exportData(dir);
		learnEmbeddings(dir, deepwalkPath);
		loadEmbeddings(dir);
	}

}

