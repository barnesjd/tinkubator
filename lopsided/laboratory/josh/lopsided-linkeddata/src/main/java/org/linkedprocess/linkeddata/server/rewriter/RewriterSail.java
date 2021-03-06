package org.linkedprocess.linkeddata.server.rewriter;

import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailWrapper;

/**
 * Author: josh
 * Date: Aug 11, 2009
 * Time: 3:30:26 PM
 */
public class RewriterSail extends SailWrapper {
    private final RewritingSchema schema;
    
    public RewriterSail(final Sail baseSail,
                        final RewritingSchema schema) {
        this.setBaseSail(baseSail);
        this.schema = schema;
    }

    @Override
    public SailConnection getConnection() throws SailException {
        return new RewriterSailConnection(this.getBaseSail().getConnection(), schema, this.getValueFactory());
    }

    @Override
    public boolean isWritable() throws SailException {
        // TODO: handle rewriting for write operations
        return false;
    }
}
