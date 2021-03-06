/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
************************************************************************
*/

package org.opencadc.inventory;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 * Artifact is the metadata record for an item (usually a file) in the storage system.
 * 
 * @author pdowler
 */
public class Artifact extends Entity {
    private static final Logger log = Logger.getLogger(Artifact.class);
    public static final String URI_BUCKET_CHARS = "0123456789abcdef";

    private URI uri;
    private String uriBucket;
    private URI contentChecksum;
    private Date contentLastModified;
    private Long contentLength;
    
    public String contentType;
    public String contentEncoding;
    
    /**
     * For use by storage site applications. This indicates the local storage identifier.
     * This value is not part of the artifact entity state and setting/modifying it does
     * not change the metaChecksum of the artifact.
     */
    public transient StorageLocation storageLocation;
    
    /**
     * For use by global inventory applications only. This indicates all sites that are known
     * to have a copy of the artifact. This value is not part of the artifact entity state 
     * and adding/removing values does not change the metaChecksum of the artifact.
     */
    public final transient Set<SiteLocation> siteLocations = new TreeSet<SiteLocation>();
    
    /**
     * Create a new artifact.
     * 
     * @param uri logical identifier
     * @param contentChecksum checksum of the content, form: {algorithm}:{hexadecimal value}
     * @param contentLastModified last-modified timestamp of the content
     * @param contentLength number of bytes in the content
     */
    public Artifact(URI uri, URI contentChecksum, Date contentLastModified, Long contentLength) {
        super();
        init(uri, contentChecksum, contentLastModified, contentLength);
    }
    
    /**
     * Reconstruct an artifact from serialized state.
     * 
     * @param id entity ID
     * @param uri logical identifier
     * @param contentChecksum checksum of the content, form: {algorithm}:{hexadecimal value}
     * @param contentLastModified last-modified timestamp of the content
     * @param contentLength number of bytes in the content
     */
    public Artifact(UUID id, URI uri, URI contentChecksum, Date contentLastModified, Long contentLength) {
        super(id);
        init(uri, contentChecksum, contentLastModified, contentLength);
    }
    
    private void init(URI uri, URI contentChecksum, Date contentLastModified, Long contentLength) {
        InventoryUtil.assertNotNull(Artifact.class, "uri", uri);
        InventoryUtil.validateArtifactURI(Artifact.class, uri);
        InventoryUtil.assertNotNull(Artifact.class, "contentChecksum", contentChecksum);
        InventoryUtil.assertNotNull(Artifact.class, "contentLastModified", contentLastModified);
        InventoryUtil.assertNotNull(Artifact.class, "contentLength", contentLength);
        if (contentLength <= 0L) {
            throw new IllegalArgumentException("invalid " + Artifact.class.getSimpleName() + ".contentLength: " + contentLength);
        }
        this.uri = uri;
        this.contentChecksum = contentChecksum;
        this.contentLastModified = contentLastModified;
        this.contentLength = contentLength;
        this.uriBucket = computeBucket(uri);
    }

    /**
     * @return the logical identifier
     */
    public URI getURI() {
        return uri;
    }
    
    /**
     * @return five character bucket tag for batch operations
     */
    public String getBucket() {
        return uriBucket;
    }
    
    /**
     * Content checksum is a URI of the form {algorithm}:{hexadecimal value}.
     * 
     * @return the content checksum
     */
    public URI getContentChecksum() {
        return contentChecksum;
    }

    /**
     * @return the last modifidied timestamp of the content
     */
    public Date getContentLastModified() {
        return contentLastModified;
    }

    /**
     * @return size of the content in bytes
     */
    public Long getContentLength() {
        return contentLength;
    }
    
    private String computeBucket(URI uri) {
        return InventoryUtil.computeBucket(uri, 5); // 5 hex characters: 16^5 = 1 megabuckets
    }

    /**
     * Compares uri values.
     * 
     * @param o object to compare to
     * @return true if the logical identifiers are equal, otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        Artifact f = (Artifact) o;
        return uri.equals(f.uri);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("[");
        sb.append(uri);
        sb.append("]");
        return sb.toString();
    }
}
