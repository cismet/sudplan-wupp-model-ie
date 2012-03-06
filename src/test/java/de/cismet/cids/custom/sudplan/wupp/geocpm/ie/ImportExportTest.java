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

import de.cismet.cids.custom.sudplan.geocpmrest.io.Rainevent;
import de.cismet.remotetesthelper.ws.rest.RemoteTestHelperClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import de.cismet.tools.ScriptRunner;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ImportExportTest tests right functionality of {@link GeoCPMImport} and {@link GeoCPMExport}.
 * For this purpose, it performs the following steps:
 *
 * 1) It creates a fresh cids database together with all tables and indexes needed for the import
 * 2) It imports the file <TEST_INPUT_FILE> with the {@link GeoCPMImport}er. NOTE: the file has to be 
 *    a gzipped file (committing files without compression would be too large). The test decompresses
 *    the file by itself.
 * 3) It exports the GeoCPM data imported before as described in the official GeoCPM interface definition to
 *    file <TEST_OUTPUT_FILE>.
 * 4) Then, both <TEST_INPUT_FILE> and <TEST_OUTPUT_FILE> are compared line by line. NOTE: The lines of both are trimmed
 *    before they are compared. The reason is that the original input and output files may have one field delimiter 
 *    (5 blanks) too much in a record. As this isn't expected in the GeoCPM interface description but the files 
 *    usually come from the standard authors themself, we just ignor this in our tests for now.
 * 5) Finally, the database created in step 1) is dropped and the output file generated by the {@link GeoCPMExport}er
 *    is deleted.
 * 
 * @author Benjamin Friedrich (benjamin.friedrich@cismet.de)
 * @version 1.0  01/2012
 */

public class ImportExportTest 
{    
    private GeoCPMImport importer;
    private GeoCPMExport exporter;
    private File         testOutFile;
    
    private static Connection CON;
    private static Statement  STMT;

    private static final String TEST_INPUT_FILE  = "GeoCPM_test.ein.gz";
    private static final String TEST_OUTPUT_FILE = "GeoCPM_test_out.ein";
    
    private static final String GEOCPMF_D = "GEOCPMF.D";
    private static final String GEOCPMI_D = "GEOCPMI.D";
    private static final String GEOCPMS_D = "GEOCPMS.D";
    
    private static final String DB_USER   = "postgres";
    private static final String DB_PWD    = "cismetz12";
    
    
    private static final String TEST_DB_NAME = "simple_geocpm_test_db2";
    private static final RemoteTestHelperClient SERVICE = new RemoteTestHelperClient();
    
    
    public ImportExportTest() {
    }

    
    @BeforeClass
    public static void setUpClass() throws Exception 
    {    
        final Properties p = new Properties();
        p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender");
        p.put("log4j.appender.Remote.remoteHost", "localhost");
        p.put("log4j.appender.Remote.port", "4445");
        p.put("log4j.appender.Remote.locationInfo", "true");
        p.put("log4j.rootLogger", "ALL,Remote");
        org.apache.log4j.PropertyConfigurator.configure(p);


//        SERVICE.dropDatabase(TEST_DB_NAME);
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
//
//        try
//        {            
//            STMT.executeUpdate("drop view geosuche;"); // removed as geometry column modification wouldn't be possible otherwise
//            STMT.execute("SELECT DropGeometryColumn('public','geom','geo_field');");
//            STMT.execute("SELECT AddGeometryColumn( 'public','geom','geo_field', -1, 'GEOMETRY', 2 );");
//            
//            // delta configuration test case
//            STMT.execute("INSERT INTO delta_configuration (description, locked, name, delta_breaking_edges, original_object) VALUES ('MyDeltaConfig dEsc', DEFAULT, 'MyDeltaConfig', 1, 1);");
//            STMT.execute("INSERT INTO delta_breaking_edge (name, height, description, original_object)	VALUES ('mydeltaBK1', 0.5, 'mydeltaBK1 desc', 1);");
//            STMT.execute("INSERT INTO delta_configuration_delta_breaking_edge (delta_configuration_reference, delta_breaking_edge) VALUES (4, 11);");
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
//                                     ImportExportTest.class.getResourceAsStream("../geocpm_db_v2.sql"))));
//        
    }

    @AfterClass 
    public static void tearDownClass() throws Exception 
    {
//        STMT.close();
//        CON.close();
        
//        if (! Boolean.valueOf(SERVICE.dropDatabase(TEST_DB_NAME))) 
//        {
//            throw new IllegalStateException("could not drop test db");
//        }
    }
    
    @Before 
    public void setUp() throws Exception
    {
        this.testOutFile = new File(TEST_OUTPUT_FILE);
    }
    
    @After
    public void tearDown() throws Exception
    {
//        this.testOutFile.delete();

//        final File geocpmFDOut = new File(GEOCPMF_D);
//        final File geocpmIDOut = new File(GEOCPMI_D);
//        final File geocpmSDOut = new File(GEOCPMS_D);
//        
//        geocpmFDOut.delete();
//        geocpmIDOut.delete();
//        geocpmSDOut.delete();
    }
    
    private int getNewestConfigId() throws Exception
    {
        ResultSet result = null;
        try
        {
            result = STMT.executeQuery("select max(id) from geocpm_configuration");
            assertTrue(result.next());
            final int newConfigId = result.getInt(1);
            result.close();
            return newConfigId;  
        }
        finally
        {
           result.close();
        }

    }
    
    
    @Test
    public void testImportExport() throws Exception
    {
        final String dbURL =   CON.getMetaData().getURL(); //  "jdbc:postgresql://192.168.100.12:5432/sudplan_geocpm_test"; 
       
//        GZIPInputStream gin = new GZIPInputStream(ImportExportTest.class.getResourceAsStream(TEST_INPUT_FILE));
//        BufferedInputStream geocpmEin = new BufferedInputStream(gin);
//        
        
        
        
        
        
        
        
        
        
        
        
        
        
//        final InputStream geocpmFD = ImportExportTest.class.getResourceAsStream(GEOCPMF_D);
//        final InputStream geocpmSD = ImportExportTest.class.getResourceAsStream(GEOCPMS_D);
//        final InputStream geocpmID = ImportExportTest.class.getResourceAsStream(GEOCPMI_D);
//        
//        
//        this.importer = new GeoCPMImport(new FileInputStream(new File("/home/bfriedrich/Desktop/geocpm/2012-02-27/DYNA-GeoCPM_120131/GeoCPM_Nullvariante_T=100a/GeoCPM.ein")),//geocpmEin,
//                                         new FileInputStream(new File("/home/bfriedrich/Desktop/geocpm/2012-02-27/DYNA-GeoCPM_120131/GeoCPM_DVWK_T=100a Nullvariante/DYNA.EIN")),
//                                         geocpmID,
//                                         geocpmFD,
//                                         geocpmSD,
//                                         "GeoCPM_Nullvariante_T=100a", // geocpm folder
//                                         "GeoCPM_DVWK_T=100a Nullvariante", // dyna folder
//                                         DB_USER, 
//                                         DB_PWD, 
//                                         dbURL);
//        this.importer.doImport();
//        
//       
//        
//        this.exporter = new GeoCPMExport(this.getNewestConfigId(), new File("/tmp/geocpm_export"), DB_USER, DB_PWD, dbURL);
//        this.exporter.doExport();
//
//        
//        final ArrayList<Double> precipitations = new ArrayList<Double>(12);
//        precipitations.add(3.00);
//        precipitations.add(3.00);
//        precipitations.add(3.00);
//        precipitations.add(13.00);
//        precipitations.add(13.00);
//        precipitations.add(13.00);
//        precipitations.add(13.00);
//        precipitations.add(13.00);
//        precipitations.add(40.00);
//        precipitations.add(20.00);
//        precipitations.add(20.00);
//        precipitations.add(15.00);
//        precipitations.add(0.00);
//        precipitations.add(0.00);
//        precipitations.add(3.00);
//        precipitations.add(3.00);
//        precipitations.add(3.00);
//
//        
//        final Rainevent rainEvent = new Rainevent(5, precipitations);
//        this.exporter.generateDYNA(rainEvent);
    
 
       
        
//           final GeoCPMAusImport ausImport = new GeoCPMAusImport(new File("/tmp/geocpm_export"), 
//                    DB_USER, DB_PWD, dbURL, "admin", "geoserver", "http://localhost:8080/geoserver", "sudplan");
//           ausImport.go();
//        
        
        
        
        
        
        
        
        
        
        
        
//        
//
//        //--- compare binary files
//        final File geocpmFDOut = new File(GEOCPMF_D);
//        final File geocpmIDOut = new File(GEOCPMI_D);
//        final File geocpmSDOut = new File(GEOCPMS_D);
//        geocpmFDOut.deleteOnExit();
//        geocpmIDOut.deleteOnExit();
//        geocpmSDOut.deleteOnExit();
        
        
//        final File geocpmFDIn  = new File(ImportExportTest.class.getResource(GEOCPMF_D).toURI());
//        assertTrue(FileUtils.contentEquals(geocpmFDIn, geocpmFDOut));
//        
//        final File geocpmIDIn  = new File(ImportExportTest.class.getResource(GEOCPMI_D).toURI());
//        assertTrue(FileUtils.contentEquals(geocpmIDIn, geocpmIDOut));
//        
//        final File geocpmSDIn  = new File(ImportExportTest.class.getResource(GEOCPMS_D).toURI());
//        assertTrue(FileUtils.contentEquals(geocpmSDIn, geocpmSDOut));        
 
        
        
//        //--- compare text file content
//        gin       = new GZIPInputStream(ImportExportTest.class.getResourceAsStream(TEST_INPUT_FILE));
//        geocpmEin = new BufferedInputStream(gin);
//        final List<String> inData  = IOUtils.readLines(geocpmEin);
//        final List<String> outData = IOUtils.readLines(new FileInputStream(this.testOutFile));
//    
//        assertEquals("Number of import and export data is different", inData.size(), outData.size());
//        
//        final int size = inData.size();
//        for(int i = 0; i < size; i++)
//        {
//            // Sometimes, there is a field delimiter at the end of a record in in the INPUT file.
//            // For now, we ignore it in our tests
//            assertEquals(inData.get(i).trim(), outData.get(i).trim());
//        }
    }
    
    
    
    private void testDyna(final Rainevent rainEvent, final String referenceFile) throws Exception
    {
        this.exporter = new GeoCPMExport(1, this.testOutFile, DB_USER, DB_PWD, CON.getMetaData().getURL());
        this.exporter.generateDYNA(rainEvent);
        
        final String parent   = this.testOutFile.getParent();
        final File   exported = new File(parent, GeoCPMExport.DYNA_FILE);
        
        exported.deleteOnExit();
        
        
        final List<String> exportedData  = IOUtils.readLines(ImportExportTest.class.getResourceAsStream(referenceFile));
        final List<String> referenceData = IOUtils.readLines(new FileInputStream(exported));
    
        assertEquals("Number of DYNA records is different", exportedData.size(), referenceData.size());
        
        final int size = referenceData.size();
        for(int i = 0; i < size; i++)
        {
            // Sometimes, there is a field delimiter at the end of a record in in the INPUT file.
            // For now, we ignore it in our tests
            assertEquals(exportedData.get(i).trim(), referenceData.get(i).trim());
        }
        
        exported.delete();
    }
    
    @Test  @Ignore
    public void testDynaExport() throws Exception
    {
         
        final ArrayList<Double> precipitations = new ArrayList<Double>(12);
        precipitations.add(3.00);
        precipitations.add(3.00);
        precipitations.add(3.00);
        precipitations.add(13.00);
        precipitations.add(13.00);
        precipitations.add(13.00);
        precipitations.add(13.00);
        precipitations.add(13.00);
        precipitations.add(40.00);
        precipitations.add(20.00);
        precipitations.add(20.00);
        precipitations.add(15.00);
        precipitations.add(0.00);
        precipitations.add(0.00);
        precipitations.add(3.00);
        precipitations.add(3.00);
        precipitations.add(3.00);

        
        final Rainevent rainEvent = new Rainevent(300, precipitations);
        
        this.testDyna(rainEvent, "DYNA.testDynaExport");
    }
    
    @Test   @Ignore
    public void testDynaTooLargeFieldValues() throws Exception
    {
         
        final ArrayList<Double> precipitations = new ArrayList<Double>(12);
        precipitations.add(1234567.89);
        precipitations.add(12345671.89);
        
        final Rainevent rainEvent = new Rainevent(300, precipitations);
        this.testDyna(rainEvent, "DYNA.testDynaTooLargeFieldValues");
    }
    
    @Test(expected=Exception.class) @Ignore
    public void testDynaTooManyRecords() throws Exception
    {
         
        final ArrayList<Double> precipitations = new ArrayList<Double>(1000);
        for(int i = 0; i < 999; i++)
        {
            precipitations.add(3.00);
            precipitations.add(3.00);
            precipitations.add(3.00);
            precipitations.add(13.00);
            precipitations.add(13.00);
            precipitations.add(13.00);
            precipitations.add(13.00);
            precipitations.add(13.00);
            precipitations.add(40.00);
            precipitations.add(20.00);
            precipitations.add(20.00);
            precipitations.add(15.00); 
        }

        // that's the one which causes too many records
        precipitations.add(1.00); 
        
        final Rainevent rainEvent = new Rainevent(300, precipitations);
        this.testDyna(rainEvent, "DYNA.testDynaExport");
    }
}
