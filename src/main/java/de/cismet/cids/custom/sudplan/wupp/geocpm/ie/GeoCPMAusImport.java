/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.sudplan.wupp.geocpm.ie;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.MessageFormat;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.cismet.cids.custom.sudplan.geoserver.AttributesAwareGSFeatureTypeEncoder;
import de.cismet.cids.custom.sudplan.geoserver.GSAttributeEncoder;
import java.io.*;

/**
 * DOCUMENT ME!
 *
 * @author   bfriedrich
 * @version  $Revision$, $Date$
 */
public final class GeoCPMAusImport {

    //~ Static fields/initializers ---------------------------------------------

    private static final String NULL = "NULL";

    private static final Pattern PATTERN_MAX = Pattern.compile("(\\d+)\\s+(\\d+\\..+)");
    private static final Pattern PATTERN_INFO = Pattern.compile(".+\\s+(.+)$");

    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+\\.?\\d+");

    private static final String CREATE_VIEW_STMT = " CREATE VIEW {1}{0} AS "
                + " select t.geom, m.water_level "
                + " from  geocpm_aus_max m, geocpm_triangle t "
                + " where m.geocpm_configuration_id = {0} "
                + " and   t.geocpm_configuration_id = {0} "
                + " and   m.geocpm_triangle_id      = t.index ";

    private static final int NUM_THREADS = 2;

    private static final String INFO_FILE = "GeoCPMInfo.aus";
    private static final String MAX_FILE = "GeoCPMMax.aus";

    private static final Logger LOG = Logger.getLogger(GeoCPMAusImport.class);

    private static final String GEOSERVER_DATASTORE = "geocpm";
    private static final String GEOSERVER_SLD = "geocpm_water_level";

    private static final String VIEW_NAME_BASE = "view_geocpm_aus_config_";


    private static final String CRS = "               PROJCS[&quot;DHDN / 3-degree Gauss-Kruger zone 2&quot;, "
                + "  GEOGCS[&quot;DHDN&quot;, "
                + "    DATUM[&quot;Deutsches Hauptdreiecksnetz&quot;, "
                + "      SPHEROID[&quot;Bessel 1841&quot;, 6377397.155, 299.1528128, AUTHORITY[&quot;EPSG&quot;,&quot;7004&quot;]], "
                + "      TOWGS84[612.4, 77.0, 440.2, -0.054, 0.057, -2.797, 2.55], "
                + "      AUTHORITY[&quot;EPSG&quot;,&quot;6314&quot;]], "
                + "    PRIMEM[&quot;Greenwich&quot;, 0.0, AUTHORITY[&quot;EPSG&quot;,&quot;8901&quot;]], "
                + "    UNIT[&quot;degree&quot;, 0.017453292519943295], "
                + "    AXIS[&quot;Geodetic longitude&quot;, EAST], "
                + "    AXIS[&quot;Geodetic latitude&quot;, NORTH], "
                + "    AUTHORITY[&quot;EPSG&quot;,&quot;4314&quot;]], "
                + "  PROJECTION[&quot;Transverse_Mercator&quot;, AUTHORITY[&quot;EPSG&quot;,&quot;9807&quot;]], "
                + "  PARAMETER[&quot;central_meridian&quot;, 6.0], "
                + "  PARAMETER[&quot;latitude_of_origin&quot;, 0.0], "
                + "  PARAMETER[&quot;scale_factor&quot;, 1.0], "
                + "  PARAMETER[&quot;false_easting&quot;, 2500000.0], "
                + "  PARAMETER[&quot;false_northing&quot;, 0.0], "
                + "  UNIT[&quot;m&quot;, 1.0], "
                + "  AXIS[&quot;Easting&quot;, EAST], ";

    private static final String BB_QUERY = " select "
                + " ST_XMIN(st_extent(geom)) as native_xmin,"
                + " ST_YMIN(st_extent(geom)) as native_ymin,"
                + " ST_XMAX(st_extent(geom)) as native_xmax,"
                + " ST_YMAX(st_extent(geom)) as native_ymax,"
                + " ST_XMIN(TRANSFORM(ST_SetSRID(st_extent(geom), 31466), 4326)) as lat_lon_xmin,"
                + " ST_YMIN(TRANSFORM(ST_SetSRID(st_extent(geom), 31466), 4326)) as lat_lon_ymin,"
                + " ST_XMAX(TRANSFORM(ST_SetSRID(st_extent(geom), 31466), 4326)) as lat_lon_xmax,"
                + " ST_YMAX(TRANSFORM(ST_SetSRID(st_extent(geom), 31466), 4326)) as lat_lon_ymax"
                + " from " + VIEW_NAME_BASE;

    //~ Instance fields --------------------------------------------------------

    private int configId;
    private File infoFile;
    private File maxFile;

    private final String user;
    private final String password;
    private final String dbUrl;
    private final File targetFolder;

    private final String restUser;
    private final String restPassword;
    private final String restUrl;
    private final String workspace;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeoCPMAusImport object.
     *
     * @param   targetFolder  configId DOCUMENT ME!
     * @param   user          DOCUMENT ME!
     * @param   password      DOCUMENT ME!
     * @param   dbUrl         DOCUMENT ME!
     * @param   restUser      DOCUMENT ME!
     * @param   restPassword  DOCUMENT ME!
     * @param   restUrl       DOCUMENT ME!
     * @param   workspace     DOCUMENT ME!
     *
     * @throws  IOException               DOCUMENT ME!
     * @throws  NullPointerException      DOCUMENT ME!
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    public GeoCPMAusImport(final File targetFolder,
            final String user,
            final String password,
            final String dbUrl,
            final String restUser,
            final String restPassword,
            final String restUrl,
            final String workspace) throws IOException {
        if (targetFolder == null) {
            throw new NullPointerException("Target folder must not be null");
        }

        if (!targetFolder.isDirectory()) {
            throw new IllegalArgumentException("Target folder " + targetFolder + " is not a directory");
        }

        this.checkString(user);
        this.checkString(password);
        this.checkString(dbUrl);
        this.checkString(restUser);
        this.checkString(restPassword);
        this.checkString(restPassword);
        this.checkString(workspace);

        this.targetFolder = targetFolder;

        this.configId = -1;
        this.user = user;
        this.password = password;
        this.dbUrl = dbUrl;

        this.restUser = restUser;
        this.restPassword = restPassword;
        this.restUrl = restUrl;
        this.workspace = workspace;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public String getLayerName() {
        if (this.configId == -1) {
            throw new IllegalStateException("No layer has been imported yet");
        }

        return VIEW_NAME_BASE + this.configId;
    }

    
    private File findResultsFolder(final File baseFolder) 
    {
        if(! baseFolder.isDirectory())
        {
            throw new IllegalArgumentException("base folder " + 
                                               baseFolder + 
                                               " does not exist");
        }
        
        // determine possible result folders
        
        final File[] possibleResultFolders = baseFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File file, final String fileName) {
                return file.isDirectory() && fileName.matches("\\d+");
            }
        });
        
        // find folder with file name representing the largest number among
        // all possible result folders
        
        if(possibleResultFolders.length == 0)
        {
            throw new IllegalArgumentException("Base folder " + baseFolder +  
                                               " does not contain a result folder");
        }
        
        if(possibleResultFolders.length == 1)
        {
            return possibleResultFolders[0];
        }
        else
        {
            File f = possibleResultFolders[0];
        
            for(int i = 1; i < possibleResultFolders.length; i++)
            {
                if(possibleResultFolders[i].getName().compareTo(f.getName()) > 0)
                {
                    f = possibleResultFolders[i];
                }
            }
        
            return f;
        }

    }
    
    
    /**
     * DOCUMENT ME!
     *
     * @throws  IOException       DOCUMENT ME!
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private void loadExportMetaData() throws IOException {
        LOG.info("Start loading export meta data...");

        final File exportMetaDataFile = new File(this.targetFolder, GeoCPMExport.META_DATA_FILE_NAME);
        this.checkFile(exportMetaDataFile);

        final Properties prop = new Properties();
        prop.load(new FileInputStream(exportMetaDataFile));

        this.configId = Integer.parseInt(prop.getProperty(GeoCPMExport.PROP_CONFIG_ID));

        final String geocpmFolder = prop.getProperty(GeoCPMExport.PROP_GEOCPM_FOLDER);
        
        final File resultsFolderFile = this.findResultsFolder(new File(this.targetFolder, geocpmFolder));
        
        if (!resultsFolderFile.exists()) {
            throw new RuntimeException("There is no results folder named " + resultsFolderFile);
        }

        this.infoFile = new File(resultsFolderFile, INFO_FILE);
        this.maxFile = new File(resultsFolderFile, MAX_FILE);

        this.checkFile(this.infoFile);
        this.checkFile(this.maxFile);

        LOG.info("Export meta data has been loaded successfully");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private void checkFile(final File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Input file " + file.getAbsolutePath() + " does not exist");
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException("No rightr permissions fopr input file " + file.getAbsolutePath());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   param  DOCUMENT ME!
     *
     * @throws  NullPointerException      DOCUMENT ME!
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private void checkString(final String param) {
        if (param == null) {
            throw new NullPointerException("Argument must not be null");
        }

        if (param.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument must not be empty");
        }
    }

    /**
     * Checks if the given value represents an INTEGER value. If not, NULL is returned.
     *
     * @param   value  possible integer value string
     *
     * @return  value, if value represents an Integer, "NULL" otherwise
     */
    private String handleParsedIntegerValue(final String value) {
        final Matcher m = PATTERN_NUMBER.matcher(value);
        if (m.matches()) {
            return value;
        } else {
            return NULL;
        }
    }

    /**
     * Checks if the given value represents an NUMERIC(precision, scale) value. If not, NULL is returned. If the value
     * does not fit in the NUMERIC(precision, scale) database table field, the biggest possible value for this field is
     * returned.
     *
     * @param   value      possible NUMERIC(precision, scale) value string
     * @param   precision  DOCUMENT ME!
     * @param   scale      DOCUMENT ME!
     *
     * @return  value, if value represents an NUMERIC(precision, scale) field OR biggest possible NUMERIC(precision,
     *          scale) value, if the given value is too big. "NULL" if given value does not represent a
     *          NUMERIC(precision, scale) at all.
     */
    private String handleParsedDecimalValue(final String value, final int precision, final int scale) {
        try {
            final BigDecimal number = new BigDecimal(value);
            if ((number.precision() <= precision) && (number.scale() <= scale)) {
                // decimal value fits in NUMERIC(precision, scale) database table field
                return value;
            } else {
                // decimal value does NOT fit in NUMERIC(precision, scale) database table field
                // -> so biggest possible value for this field is returned
                final char[] maxRepresentation = new char[precision + 1];

                int i;
                final int numElem = precision - scale;
                for (i = 0; i < numElem; i++) {
                    maxRepresentation[i] = '9';
                }

                maxRepresentation[i++] = '.';

                for (; i < maxRepresentation.length; i++) {
                    maxRepresentation[i] = '9';
                }

                return String.valueOf(maxRepresentation);
            }
        } catch (final NumberFormatException e) {
            // Value is not a decimal value (e.g. -1.#R, 1.#INF000000, ... ) -> handled as NULL
            return NULL;
        }
    }

    /**
     * Reads-in the entire given file (blockwise) to main memory. The returned BufferedReader is accessing the in-memory
     * data.
     *
     * @param   file  file to be read-in
     *
     * @return  Reader for accessing the read-in data
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private BufferedReader readInFile(final File file) throws Exception {
        final FileChannel fc = new FileInputStream(file).getChannel();
        final byte[] values = new byte[(int)file.length()];
        final ByteBuffer buff = ByteBuffer.wrap(values);
        fc.read(buff);
        fc.close();

        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(values)));
    }

    /**
     * Imports the GeoCPMInfo.aus file.
     *
     * @param   con  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void importGeoCPMInfo(final Connection con) throws Exception {
        LOG.info("Start import of info file " + this.infoFile);

        // read-in file + extract information
        final BufferedReader reader = this.readInFile(this.infoFile);
        Matcher m;
        String line;
        int i = 0;
        final String[] values = new String[18];
        while ((line = reader.readLine()) != null) {
            m = PATTERN_INFO.matcher(line);
            if (m.matches()) {
                values[i++] = m.group(1);
            } else {
                LOG.warn("Line does not match pattern -> IGNORED: " + line);
            }
        }
        reader.close();

        // execute corresponding update statement
        final Statement stmt = con.createStatement();
        stmt.executeUpdate(
            "INSERT INTO geocpm_aus_info "
                    + "VALUES("
                    + this.configId
                    + ','  // configuration id
                    + this.handleParsedIntegerValue(values[0])
                    + ','  // number of elements
                    + this.handleParsedIntegerValue(values[1])
                    + ','  // number of edges
                    + this.handleParsedIntegerValue(values[2])
                    + ','  // number of calcualtion steps
                    + this.handleParsedDecimalValue(values[3], 10, 2)
                    + ','  // volume of Drain/Source in l
                    + this.handleParsedDecimalValue(values[4], 10, 2)
                    + ','  // volume of Street in l
                    + this.handleParsedDecimalValue(values[5], 10, 2)
                    + ','  // volume of all elements in l
                    + this.handleParsedDecimalValue(values[6], 10, 2)
                    + ','  // volume of all losses in l
                    + this.handleParsedDecimalValue(values[7], 10, 2)
                    + ','  // volume exchange DYNA -> GeoCPM in l
                    + this.handleParsedDecimalValue(values[8], 10, 2)
                    + ','  // volume of exchange GeoCPM -> DYNA in l
                    + this.handleParsedDecimalValue(values[9], 10, 2)
                    + ','  // rain on surface elements in mm
                    + this.handleParsedDecimalValue(values[10], 10, 2)
                    + ','  // time total
                    + this.handleParsedDecimalValue(values[11], 10, 2)
                    + ','  // time step calculation
                    + this.handleParsedDecimalValue(values[12], 10, 2)
                    + ','  // time boundary conditions
                    + this.handleParsedDecimalValue(values[13], 10, 2)
                    + ','  // time boundary conditions Source and Drain
                    + this.handleParsedDecimalValue(values[14], 10, 2)
                    + ','  // time boundary conditions manholes
                    + this.handleParsedDecimalValue(values[15], 10, 2)
                    + ','  // time boundary conditions triangle elements
                    + this.handleParsedDecimalValue(values[16], 10, 2)
                    + ','  // Saint-Venant'sche DGL
                    + this.handleParsedDecimalValue(values[17], 10, 2)
                    + ");" // time overhead
                    );

        LOG.info("Import of info file " + this.infoFile + " has been finished successfully");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   con  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void createView(final Connection con) throws Exception {
        LOG.info("Start view creation...");
        final Statement stmt = con.createStatement();
        stmt.executeUpdate(MessageFormat.format(CREATE_VIEW_STMT, this.configId, VIEW_NAME_BASE));
        stmt.close();
        LOG.info("View has been created successfully");
    }

    /**
     * Imports the GeoCPMMax.aus file.
     *
     * @param   con  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void importGeoCPMMax(final Connection con) throws Exception {
        LOG.info("Start import of max file " + this.maxFile);

        final StringBuilder builder = new StringBuilder(((int)this.maxFile.length()) << 1);

        builder.append("INSERT INTO geocpm_aus_max VALUES ");

        final BufferedReader reader = this.readInFile(this.maxFile);
        Matcher m;
        String line;

        boolean isFirst = true;

        while ((line = reader.readLine()) != null) {
            m = PATTERN_MAX.matcher(line);
            if (m.matches()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    builder.append(',');
                }

                builder.append('(').append(this.configId)                              // configuration id
                .append(',').append(m.group(1))                                        // triangle index
                .append(',').append(this.handleParsedDecimalValue(m.group(2), 20, 10)) // water level
                .append(')');
            } else {
                LOG.warn("Line does not match pattern -> IGNORED: " + line);
            }
        }
        reader.close();

        if (isFirst) {
            LOG.warn("No records were found in " + this.maxFile);
        } else {
            builder.append(';');
            final Statement stmt = con.createStatement();
            stmt.executeUpdate(builder.toString());
        }

        LOG.info("Import of max file " + this.maxFile + " has been finished successfully");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   con  DOCUMENT ME!
     *
     * @throws  Exception         DOCUMENT ME!
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private void importToGeoServer(final Connection con) throws Exception {
        LOG.info("Start GeoServer import...");

        final GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(
                this.restUrl,
                this.restUser,
                this.restPassword);

        final AttributesAwareGSFeatureTypeEncoder featureType = new AttributesAwareGSFeatureTypeEncoder();
        featureType.setName(VIEW_NAME_BASE + this.configId);  // view name as feature type name
        featureType.setEnabled(true);
        featureType.setSRS("EPSG:31466");
        featureType.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        featureType.setTitle(VIEW_NAME_BASE + this.configId); // view name as feature type title

        GSAttributeEncoder attribute = new GSAttributeEncoder();
        attribute.addEntry("name", "geom");
        attribute.addEntry("minOccurs", "0");
        attribute.addEntry("maxOccurs", "1");
        attribute.addEntry("nillable", "false");
        attribute.addEntry("binding", "com.vividsolutions.jts.geom.Geometry");
        featureType.addAttribute(attribute);

        attribute = new GSAttributeEncoder();
        attribute.addEntry("name", "water_level");
        attribute.addEntry("minOccurs", "0");
        attribute.addEntry("maxOccurs", "1");
        attribute.addEntry("nillable", "true");
        attribute.addEntry("binding", "java.math.BigDecimal");
        featureType.addAttribute(attribute);

        // retrieve bounding boxes from generated view

        final Statement query = con.createStatement();
        final ResultSet result = query.executeQuery(BB_QUERY + this.configId);

        if (!result.next()) {
            throw new RuntimeException("view " + VIEW_NAME_BASE + this.configId + " does not deliver any records");
        }

        featureType.setNativeBoundingBox(result.getDouble("native_xmin"),
            result.getDouble("native_ymin"),
            result.getDouble("native_xmax"),
            result.getDouble("native_ymax"),
            CRS);

        featureType.setLatLonBoundingBox(result.getDouble("lat_lon_xmin"),
            result.getDouble("lat_lon_ymin"),
            result.getDouble("lat_lon_xmax"),
            result.getDouble("lat_lon_ymax"),
            CRS);

        final GSLayerEncoder layer = new GSLayerEncoder();
        layer.setEnabled(true);
        layer.setDefaultStyle(GEOSERVER_SLD);

        if (!publisher.publishDBLayer(this.workspace, GEOSERVER_DATASTORE, featureType, layer)) {
            throw new RuntimeException("GeoServer import was not successful");
        }

        LOG.info("GeoServer import has been successful");
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    public void go() {
        LOG.info("Start of import process...");

        Connection con = null;
        try {
            this.loadExportMetaData();

            final ExecutorService execService = Executors.newFixedThreadPool(NUM_THREADS);

            con = DriverManager.getConnection(dbUrl, user, password);
            final Connection finalCon = con;

            // start thread for importing GeoCPM info file
            execService.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            importGeoCPMInfo(finalCon);
                        } catch (final Exception ex) {
                            LOG.error("An error occurred while importing info file", ex);
                            throw new RuntimeException(ex);
                        }
                    }
                });

            // start thread for importing GeoCPM max file
            execService.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            importGeoCPMMax(finalCon);
                        } catch (final Exception ex) {
                            LOG.error("An error occurred while importing max file", ex);
                            throw new RuntimeException(ex);
                        }
                    }
                });

            // start thread for creating view needed by WMS
            execService.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            createView(finalCon);
                        } catch (final Exception ex) {
                            LOG.error("An error occurred while creating view", ex);
                            throw new RuntimeException(ex);
                        }
                    }
                });

            LOG.info("Waiting until all threads are finished...");

            // wait for at most 1 hour for termination
            execService.shutdown();
            execService.awaitTermination(1L, TimeUnit.HOURS);

            // now start final import to geoserver
            this.importToGeoServer(con);

            LOG.info("Import has been finished successfully");
        } catch (final Exception e) {
            LOG.error("An error has occurred", e);
            // TODO better exception type
            throw new RuntimeException(e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (final SQLException ex) {
                    LOG.warn("An error has occurred while closing the connection", ex);
                }
            }
        }
    }
}
