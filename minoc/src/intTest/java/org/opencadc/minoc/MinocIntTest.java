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

package org.opencadc.minoc;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.HttpDelete;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.net.HttpUpload;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author majorb
 *
 */
public class MinocIntTest {
    
    private static final Logger log = Logger.getLogger(MinocIntTest.class);
    private static final URI MINOC_SERVICE_ID = URI.create("ivo://cadc.nrc.ca/minoc");
    // TODO: Move this to Standards.java
    private static final URI MINOC_STANDARD_ID = URI.create("vos://cadc.nrc.ca~vospace/CADC/std/inventory#artifacts-1.0");

    private URL anonURL;
    
    static {
        Log4jInit.setLevel("org.opencadc.minoc", Level.INFO);
    }
    
    public MinocIntTest() {
        RegistryClient regClient = new RegistryClient();
        anonURL = regClient.getServiceURL(MINOC_SERVICE_ID, MINOC_STANDARD_ID, AuthMethod.ANON);
        log.info("anonURL: " + anonURL);
    }
    
    @Test
    public void testAllMethodsSimple() {
        try {
            
            String data = "abcdefghijklmnopqrstuvwxyz";
            URI artifactURI = URI.create("cadc:TEST/file.fits");
            URL artifactURL = new URL(anonURL + "/" + artifactURI.toString());
            String encoding = "test-encoding";
            String type = "test-type";
            
            // put
            InputStream in = new ByteArrayInputStream(data.getBytes());
            HttpUpload put = new HttpUpload(in, artifactURL);
            put.setContentEncoding(encoding);
            put.setContentType(type);
            put.run();
            Assert.assertNull(put.getThrowable());
            
            // get
            OutputStream out = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(artifactURL, out);
            get.run();
            Assert.assertNull(get.getThrowable());
            String contentMD5 = get.getContentMD5();
            long contentLength = get.getContentLength();
            String contentType = get.getContentType();
            String contentEncoding = get.getContentEncoding();
            Assert.assertEquals(getMd5(data.getBytes()), contentMD5);
            Assert.assertEquals(data.getBytes().length, contentLength);
            Assert.assertEquals(type, contentType);
            Assert.assertEquals(encoding, contentEncoding);
            
            // update
            // TODO: add update to artifactURI when functionality available
            String newEncoding = "test-encoding-2";
            String newType = "test-type-2";
            Map<String,Object> params = new HashMap<String,Object>(2);
            params.put("contentEncoding", newEncoding);
            params.put("contentType", newType);
            HttpPost post = new HttpPost(artifactURL, params, false);
            post.run();
            Assert.assertNull(post.getThrowable());
            
            // head
            HttpDownload head = new HttpDownload(artifactURL, out);
            head.setHeadOnly(true);
            head.run();
            Assert.assertNull(head.getThrowable());
            contentMD5 = head.getContentMD5();
            contentLength = head.getContentLength();
            contentType = head.getContentType();
            contentEncoding = head.getContentEncoding();
            Assert.assertEquals(getMd5(data.getBytes()), contentMD5);
            Assert.assertEquals(data.getBytes().length, contentLength);
            Assert.assertEquals(newType, contentType);
            Assert.assertEquals(newEncoding, contentEncoding);
            
            // delete
            HttpDelete delete = new HttpDelete(artifactURL, false);
            delete.run();
            Assert.assertNull(delete.getThrowable());
            
            // get
            get = new HttpDownload(artifactURL, out);
            get.run();
            Throwable throwable = get.getThrowable();
            Assert.assertNotNull(throwable);
            Assert.assertTrue(throwable instanceof FileNotFoundException);
            
        } catch (Throwable t) {
            log.error("unexpected throwable", t);
            Assert.fail("unexpected throwable: " + t);
        }
    }
    
    public static String getMd5(byte[] input) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream in = new ByteArrayInputStream(input);
        DigestInputStream dis = new DigestInputStream(in, md);
        int bytesRead = dis.read();
        byte[] buf = new byte[512];
        while (bytesRead > 0) {
            bytesRead = dis.read(buf);
        }
        byte[] digest = md.digest();
        return HexUtil.toHex(digest);
    }
}
