package com.tinkerpop.tinkubator.pgsail;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A Sail implementation which provides an RDF view of any Blueprints graph.
 * @see <a href="https://github.com/tinkerpop/tinkubator/wiki/PropertyGraphSail">PropertyGraphSail</a>
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class PropertyGraphSail implements Sail {
    public static final String PROPERTY_NS = "http://tinkerpop.com/pgm/property/";
    public static final String VERTEX_NS = "http://tinkerpop.com/pgm/vertex/";
    public static final String EDGE_NS = "http://tinkerpop.com/pgm/edge/";
    public static final String ONTOLOGY_NS = "http://tinkerpop.com/pgm/ontology#";

    public static final URI
            EDGE = new URIImpl(ONTOLOGY_NS + "Edge"),
            VERTEX = new URIImpl(ONTOLOGY_NS + "Vertex"),
            ID = new URIImpl(ONTOLOGY_NS + "id"),
            LABEL = new URIImpl(ONTOLOGY_NS + "label"),
            HEAD = new URIImpl(ONTOLOGY_NS + "head"),
            TAIL = new URIImpl(ONTOLOGY_NS + "tail");

    public enum Properties {
        sharability,
        weight,
    }

    private final PropertyGraphContext context;

    /**
     * Instantiates a Sail based on a given Graph.
     *
     * @param graph the Blueprints Graph to expose as an RDF dataset
     */
    public PropertyGraphSail(final Graph graph) {
        context = new PropertyGraphContext(graph, new PropertyGraphValueFactory(graph));

        //graph.getIndex("", Vertex.class);

    }

    public void setVertexPropertyIndex(final String label,
                                       final String indexName) {
        /*
        Index<Vertex> i = context.graph.getIndex(indexName, Vertex.class);
        if (null == i) {
            throw new IllegalArgumentException("no such vertex index: " + indexName);
        }

        context.indices.put(label, i);
        */
    }

    public void setDataDir(File file) {
        throw new UnsupportedOperationException();
    }

    public File getDataDir() {
        throw new UnsupportedOperationException();
    }

    public void initialize() throws SailException {
        // Do nothing.
    }

    public void shutDown() throws SailException {
        // Do nothing.
    }

    public boolean isWritable() throws SailException {
        return false;
    }

    public SailConnection getConnection() throws SailException {
        return new PropertyGraphSailConnection(context);
    }

    public ValueFactory getValueFactory() {
        return context.valueFactory;
    }



    static class PropertyGraphContext {
        public final Graph graph;

        public final ValueFactory valueFactory;

        public final Map<String, Index<Vertex>> indices = new HashMap<String, Index<Vertex>>();

        public PropertyGraphContext(final Graph graph,
                                    final ValueFactory valueFactory) {
            this.graph = graph;
            this.valueFactory = valueFactory;
        }
    }
}
