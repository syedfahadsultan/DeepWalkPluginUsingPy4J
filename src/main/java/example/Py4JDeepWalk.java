
package example;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.GraphDatabaseService;
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
import py4j.GatewayServer;

import example.EmbeddingsInterface;

import java.nio.file.Paths;
/**
 * This is an example how you can create a simple user-defined function for Neo4j.
 */
public class Py4JDeepWalk
{
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

	private void getEmbeddings(String path) throws IOException{
		log.info("Deepwalk: Exporting graph as an edgelist csv");

        GatewayServer server = new GatewayServer();
        server.start();
        EmbeddingsInterface embeddings = (EmbeddingsInterface) server.getPythonServerEntryPoint(new Class[] { EmbeddingsInterface.class });
        
        
        ArrayList<String> list = new ArrayList<>();
  
        // Iterate through the iterable to 
        // add each element into the collection 
        for (Relationship rel : db.getAllRelationships()){
            long startNodeId = rel.getStartNodeId();
            long endNodeId = rel.getEndNodeId();
            list.add(startNodeId+"_"+endNodeId);
        }

        // HashMap<String, String> embs = embeddings.getEmbeddings(list);
        // for(Map.Entry m:embs.entrySet()){    
        //    Node n = db.getNodeById(Long.parseLong(m.getKey().toString()));
        //    n.setProperty("emb", m.getValue()); 
        // }  

        String embs = embeddings.getEmbeddings(list);
        String[] vectors = embs.split("\n");
        for(String vector: vectors){
            log.info(vector);
            String[] keyval = vector.split("\t");
            Node n = db.getNodeById(Long.parseLong(keyval[0]));
            n.setProperty("emb", keyval[1]);
        }


    }

	@Procedure(name="example.deepWalk2", mode=Mode.WRITE)
	@Description("Calls deepwalk from bash and writes back embeddings as node properties")
    public void deepWalk2() throws IOException, FileNotFoundException, InterruptedException, KernelException{

        // String deepwalkPath = System.getenv("DEEPWALK");
        String condaPath = System.getenv("CONDA");
        String dir = System.getenv("NEO4J_HOME");
        log.info("Deepwalk - Path: "+condaPath);
        log.info("Deepwalk - Dir: "+dir);

		getEmbeddings(dir);
	}

}

