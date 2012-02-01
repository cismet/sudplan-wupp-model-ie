/*
 * Copyright (C) 2011 cismet GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cismet.cids.custom.sudplan.wupp.geocpm.ie;

import de.cismet.cids.custom.sudplan.geoserver.AttributesAwareGSFeatureTypeEncoder;
import de.cismet.cids.custom.sudplan.geoserver.GSAttributeEncoder;
import de.cismet.remotetesthelper.ws.rest.RemoteTestHelperClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import de.cismet.tools.ScriptRunner;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author Benjamin Friedrich (benjamin.friedrich@cismet.de)
 * @version 1.0  01/2012
 */
@Ignore
public class GeoCPMAusImportTest 
{    
    private File         testImportFolder;
    
    
    private static Connection CON;
    private static Statement  STMT;

    private static final String TEST_GEOCPM_FILE  = "GeoCPM.ein";
    
    private static final String DB_USER   = "postgres";
    private static final String DB_PWD    = "cismetz12";
    
    
    private static final String TEST_DB_NAME = "simple_geocpm_test_db";
    private static final RemoteTestHelperClient SERVICE = new RemoteTestHelperClient();
    
    
    
    private static final String RESTURL   = "http://sudplanwp6.cismet.de/geoserver";
    private static final String RESTUSER  = "admin";
    private static final String RESTPW    = "cismetz12";
    private static final String WORKSPACE = "sudplan"; 
    
    
    public GeoCPMAusImportTest() {
    }

    @BeforeClass @Ignore
    public static void setUpClass() throws Exception 
    {
        final Properties p = new Properties();
        p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender");
        p.put("log4j.appender.Remote.remoteHost", "localhost");
        p.put("log4j.appender.Remote.port", "4445");
        p.put("log4j.appender.Remote.locationInfo", "true");
        p.put("log4j.rootLogger", "ALL,Remote");
        org.apache.log4j.PropertyConfigurator.configure(p);

//        
//        SERVICE.dropDatabase(TEST_DB_NAME); 
//
//        
//        
//        if (! Boolean.valueOf(SERVICE.initCidsSystem(TEST_DB_NAME))) 
//        {
//            throw new IllegalStateException("cannot initilise test db");
//        }
//        
        CON  = SERVICE.getConnection(TEST_DB_NAME);
        STMT = CON.createStatement();    
//
//        try
//        {
//            STMT.executeUpdate("drop view geosuche;"); // removed as geometry column modification wouldn't be possible otherwise
//            STMT.execute("SELECT DropGeometryColumn('public','geom','geo_field');");
//            STMT.execute("SELECT AddGeometryColumn( 'public','geom','geo_field', -1, 'GEOMETRY', 2 );");
//            
//        }
//        catch(final SQLException e)
//        {
//            e.printStackTrace();
//            e.getNextException().printStackTrace();
//            throw e;
//        }
//        
//        final ScriptRunner runner = new ScriptRunner(CON, true, true);
//        runner.runScript(new BufferedReader(
//                                 new InputStreamReader(                       
//                                     GeoCPMAusImportTest.class.getResourceAsStream("../geocpm_db_v2.sql"))));
        
    }

    @AfterClass @Ignore
    public static void tearDownClass() throws Exception 
    {
        STMT.close();
        CON.close();
        
//        if (! Boolean.valueOf(SERVICE.dropDatabase(TEST_DB_NAME))) 
//        {
//            throw new IllegalStateException("could not drop test db");
//        }
    }
    
    @Before @Ignore
    public void setUp() throws Exception
    {
        this.testImportFolder = new File("/home/bfriedrich/NetBeansProjects/wupp-model-ie/src/main/resources/de/cismet/cids/custom/sudplan/wupp/geocpm/ie");
    }
    
    
    @Test @Ignore
    public void testImportExport() throws Exception
    {
        final String dbURL =  CON.getMetaData().getURL();
        
//        final BufferedInputStream bin = new BufferedInputStream(GeoCPMAusImportTest.class.getResourceAsStream(TEST_GEOCPM_FILE));
//        final GeoCPMImport geocpmImporter = new GeoCPMImport(bin, DB_USER, DB_PWD, dbURL);
//        geocpmImporter.doImport();
        
        
        
        final GeoCPMAusImport importer = new GeoCPMAusImport( this.testImportFolder.getAbsolutePath(), 
                                                              DB_USER, 
                                                              DB_PWD, 
                                                              dbURL, 
                                                              RESTUSER, 
                                                              DB_PWD, 
                                                              RESTURL, 
                                                              WORKSPACE);
        
        importer.go();
  
    }
    
  
 
}
