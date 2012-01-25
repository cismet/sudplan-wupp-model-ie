/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.sudplan.wupp.geocpm.ie;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

/**
 * GeoCPMExport exports GeoCPM data which was imported by {@link GeoCPMImport} before to a format which is compliant to
 * the GeoCPM interface description v0.5 (5th October 2011).
 *
 * <p>NOTE: GeoCPMExport already implements the handling of NULL values which will be defined in the next GeoCPM
 * standard namely that NULL values are represented by <NULL_TOKEN_FILE>. However, we only consider values for this
 * handling which are not of type boolean. The reason is that the current implementation of the GeoCPMExport relies on
 * these primitive boolean types and it is very unlikely (at least, at this moment) that those values might become NULL.
 * To see the current handling of NULL values, consider the handleValue(...) methods.</p>
 *
 * <p>RAINCURVES are not exported by GeoCPMExport. This data will be put in the resulting export output file by another
 * tool. That's why GeoCPMExport just output "RAINCURVE 0\n\n".</p>
 *
 * @author   Benjamin Friedrich (benjamin.friedrich@cismet.de)
 * @version  1.0 01/2012
 */
public class GeoCPMExport {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(GeoCPMExport.class);

    public static final String SECTION_CONFIG = "Configuration";      // NOI18N
    public static final String SECTION_POINTS = "POINTS";             // NOI18N
    public static final String SECTION_TRIANGLES = "TRIANGLES";       // NOI18N
    public static final String SECTION_CURVES = "CURVES";             // NOI18N
    public static final String SECTION_SOURCE_DRAIN = "SOURCE-DRAIN"; // NOI18N
    public static final String SECTION_MANHOLES = "MANHOLES";         // NOI18N
    public static final String SECTION_MARKED = "MARKED";             // NOI18N
    public static final String SECTION_RAINCURVE = "RAINCURVE";       // NOI18N
    public static final String SECTION_BK_CONNECT = "BK-CONNECT";     // NOI18N

    public static final String CALC_BEGIN = "Beginning of calculation";             // NOI18N
    public static final String CALC_END = "End of calculation";                     // NOI18N
    public static final String WRITE_NODE = "Write full result list Node";          // NOI18N
    public static final String WRITE_EDGE = "Write full result list Edge";          // NOI18N
    public static final String LAST_VALUES = "Last Values";                         // NOI18N
    public static final String SAVE_MARKED = "Save Marked";                         // NOI18N
    public static final String MERGE_TRIANGLES = "Merge triangles";                 // NOI18N
    public static final String MIN_CALC_TRIANGLE_SIZE = "Min. calc. triangle size"; // NOI18N
    public static final String TIME_STEP_RESTRICTION = "Time step restriction";     // NOI18N
    public static final String SAVE_VELOCITY_CURVES = "Save velosity curves";       // NOI18N
    public static final String SAVE_FLOW_CURVES = "Save flow curves";               // NOI18N
    public static final String RESULT_SAVE_LIMIT = "Result save limit";             // NOI18N
    public static final String NUMBER_OF_THREADS = "Number of threads";             // NOI18N
    public static final String Q_IN = "Ansatz Q in";                                // NOI18N
    public static final String Q_OUT = "Ansatz Q out";                              // NOI18N

    public static final String FIELD_SEP = "     "; // NOI18N
    public static final char EOL = '\n';
    public static final String COL = ": ";

    public static final String NULL_TOKEN_FILE = "-1.#R";

    private static final DecimalFormatSymbols DCFS = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
    private static final DecimalFormat DCF2 = new DecimalFormat("#0.00", DCFS);
    private static final DecimalFormat DCF3 = new DecimalFormat("#0.000", DCFS);
    private static final DecimalFormat DCF8 = new DecimalFormat("#0.00000000", DCFS);

    //~ Instance fields --------------------------------------------------------

    private final transient DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // NOI18N

    private final transient File outFile;
    private final transient int configId;

    private final transient StringBuilder prepContent;

    private final transient String user;
    private final transient String password;
    private final transient String dbUrl;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeoCPMImport object.
     *
     * @param   configId  input DOCUMENT ME!
     * @param   outFile   DOCUMENT ME!
     * @param   user      DOCUMENT ME!
     * @param   password  DOCUMENT ME!
     * @param   dbUrl     DOCUMENT ME!
     *
     * @throws  ClassNotFoundException    DOCUMENT ME!
     * @throws  NullPointerException      DOCUMENT ME!
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    public GeoCPMExport(final int configId,
            final File outFile,
            final String user,
            final String password,
            final String dbUrl) throws ClassNotFoundException {
        if (outFile == null) {
            throw new NullPointerException("Given output file must not be null");
        }

        if ((user == null) || (password == null) || (dbUrl == null)) {
            throw new NullPointerException("At least one of the db params is null");
        }

        if (outFile.exists()) {
            throw new IllegalArgumentException("Given target file " + outFile + " does already exist");
        }

        this.configId = configId;
        this.outFile = outFile;
        this.prepContent = new StringBuilder(30 * 1024 * 1024); // allocate 60 MB (1 char = 2 Byte)
        this.user = user;
        this.password = password;
        this.dbUrl = dbUrl;

        Class.forName("org.postgresql.Driver"); // NOI18N
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   dcf    format DOCUMENT ME!
     * @param   value  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String handleValue(final DecimalFormat dcf, final BigDecimal value) {
        return (value == null) ? NULL_TOKEN_FILE : dcf.format(value.doubleValue());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   value  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String handleValue(final String value) {
        return (value == null) ? NULL_TOKEN_FILE : value;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   value  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String handleValue(final Timestamp value) {
        return (value == null) ? NULL_TOKEN_FILE : this.dateFormat.format(value);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveConfigData(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING POINTS");

        ResultSet result = null;
        try {
            result = stmt.executeQuery("select * from geocpm_configuration where id =" + this.configId);
            if (!result.next()) {
                throw new SQLException("There are no configuration records for configuration id " + this.configId);
            }

            final Timestamp calcBegin = result.getTimestamp("calc_begin");
            final Timestamp calcEnd = result.getTimestamp("calc_end");
            final boolean writeNode = result.getBoolean("write_node");
            final boolean writeEdge = result.getBoolean("write_edge");
            final boolean lastValues = result.getBoolean("last_values");
            final boolean saveMarked = result.getBoolean("save_marked");
            final boolean mergeTriangles = result.getBoolean("merge_triangles");
            final BigDecimal minCalcTriangeSize = result.getBigDecimal("min_calc_triangle_size");
            final boolean timeStepRestriction = result.getBoolean("time_step_restriction");
            final boolean saveVelocityCurves = result.getBoolean("save_velocity_curves");
            final boolean saveFlowCurves = result.getBoolean("save_flow_curves");
            final BigDecimal resultSaveLimit = result.getBigDecimal("result_save_limit");
            final String numberOfThreads = this.handleValue(result.getString("number_of_threads"));
            final String qIn = this.handleValue(result.getString("q_in"));
            final String qOut = this.handleValue(result.getString("q_out"));

            final char YES = 'y';
            final char NO = 'n';
            this.prepContent.append(SECTION_CONFIG).append(EOL);
            this.prepContent.append(CALC_BEGIN).append(COL).append(this.handleValue(calcBegin)).append(EOL);
            this.prepContent.append(CALC_END).append(COL).append(this.handleValue(calcEnd)).append(EOL);
            this.prepContent.append(EOL);
            this.prepContent.append(WRITE_NODE).append(COL).append(writeNode ? YES : NO).append(EOL);
            this.prepContent.append(WRITE_EDGE).append(COL).append(writeEdge ? YES : NO).append(EOL);
            this.prepContent.append(LAST_VALUES).append(COL).append(lastValues ? YES : NO).append(EOL);
            this.prepContent.append(SAVE_MARKED).append(COL).append(saveMarked ? YES : NO).append(EOL);
            this.prepContent.append(MERGE_TRIANGLES).append(COL).append(mergeTriangles ? YES : NO).append(EOL);
            this.prepContent.append(MIN_CALC_TRIANGLE_SIZE)
                    .append(COL)
                    .append(this.handleValue(DCF8, minCalcTriangeSize)) // DCF8.format(minCalcTriangeSize))
            .append(EOL);
            this.prepContent.append(TIME_STEP_RESTRICTION)
                    .append(COL)
                    .append(timeStepRestriction ? YES : NO)
                    .append(EOL);
            this.prepContent.append(SAVE_VELOCITY_CURVES).append(COL).append(saveVelocityCurves ? YES : NO).append(EOL);
            this.prepContent.append(SAVE_FLOW_CURVES).append(COL).append(saveFlowCurves ? YES : NO).append(EOL);
            this.prepContent.append(RESULT_SAVE_LIMIT)
                    .append(COL)
                    .append(this.handleValue(DCF8, resultSaveLimit))
                    .append(EOL);
            this.prepContent.append(NUMBER_OF_THREADS).append(COL).append(numberOfThreads).append(EOL);
            this.prepContent.append(Q_IN).append(COL).append(qIn).append(EOL);
            this.prepContent.append(Q_OUT).append(COL).append(qOut).append(EOL);
            this.prepContent.append(EOL);
        } finally {
            result.close();
        }

        LOG.info("CONFIGURATION DATA HAS BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrievePoints(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING POINTS");

        int count = 0;
        ResultSet result = null;
        try {
            result = stmt.executeQuery("SELECT index, ST_X(geom), ST_Y(geom), ST_Z(geom) "
                            + "FROM geocpm_point where geocpm_configuration_id = " + this.configId
                            + "ORDER BY index");

            BigDecimal x;
            BigDecimal y;
            BigDecimal z;
            String index;

            final StringBuilder tmpContent = new StringBuilder(this.prepContent.capacity() / 3);

            while (result.next()) {
                index = result.getString(1);
                x = result.getBigDecimal(2);
                y = result.getBigDecimal(3);
                z = result.getBigDecimal(4);

                tmpContent.append(this.handleValue(index))
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, x))
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, y))
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, z))
                        .append(EOL);

                count++;
            }

            this.prepContent.append(SECTION_POINTS).append(' ').append(count).append(EOL);
            this.prepContent.append(tmpContent);
            this.prepContent.append(EOL);
        } finally {
            result.close();
        }

        LOG.info("POINTS HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveTriangles(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING TRIANGLES");

        int count = 0;
        ResultSet result = null;
        try {
            result = stmt.executeQuery(
                    "select t.index as index, p1.index as index_a, p2.index as index_b, p3.index as index_c,"
                            + "t.neighbour_a_id, t.neighbour_b_id,    t.neighbour_c_id,"
                            + "t.roughness, t.loss, t.be_height_a, t.be_height_b, t.be_height_c "
                            + "from geocpm_triangle t, geocpm_point p1, geocpm_point p2,  geocpm_point p3 "
                            + "where t.geocpm_configuration_id = "
                            + this.configId
                            + "and   p1.id = t.geocpm_point_a_id "
                            + "and   p2.id = t.geocpm_point_b_id "
                            + "and   p3.id = t.geocpm_point_c_id "
                            + "order by index");

            String index;
            String indexA;
            String indexB;
            String indexC;
            String neighbourA;
            String neighbourB;
            String neighbourC;
            BigDecimal roughness;
            BigDecimal loss;
            BigDecimal beHeightA = BigDecimal.ZERO;
            BigDecimal beHeightB = BigDecimal.ZERO;
            BigDecimal beHeightC = BigDecimal.ZERO;
            boolean areHeightsNull;

            final StringBuilder tmpContent = new StringBuilder(this.prepContent.capacity() / 2);

            while (result.next()) {
                index = this.handleValue(result.getString("index"));
                indexA = this.handleValue(result.getString("index_a"));
                indexB = this.handleValue(result.getString("index_b"));
                indexC = this.handleValue(result.getString("index_c"));
                neighbourA = this.handleValue(result.getString("neighbour_a_id"));
                neighbourB = this.handleValue(result.getString("neighbour_b_id"));
                neighbourC = this.handleValue(result.getString("neighbour_c_id"));
                roughness = result.getBigDecimal("roughness");
                loss = result.getBigDecimal("loss");

                // assumption is that either all height_xxx columns have values or none of them
                areHeightsNull = (result.getObject("be_height_a") == null)
                            || (result.getObject("be_height_b") == null)
                            || (result.getObject("be_height_c") == null);

                if (!areHeightsNull) {
                    beHeightA = result.getBigDecimal("be_height_a");
                    beHeightB = result.getBigDecimal("be_height_b");
                    beHeightC = result.getBigDecimal("be_height_c");
                }

                tmpContent.append(index)
                        .append(FIELD_SEP)
                        .append(indexA)
                        .append(FIELD_SEP)
                        .append(indexB)
                        .append(FIELD_SEP)
                        .append(indexC)
                        .append(FIELD_SEP)
                        .append(neighbourA)
                        .append(FIELD_SEP)
                        .append(neighbourB)
                        .append(FIELD_SEP)
                        .append(neighbourC)
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, roughness))
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, loss));

                if (areHeightsNull) {
                    tmpContent.append(EOL);
                } else {
                    tmpContent.append(FIELD_SEP)
                            .append(this.handleValue(DCF3, beHeightA))
                            .append(FIELD_SEP)
                            .append(this.handleValue(DCF3, beHeightB))
                            .append(FIELD_SEP)
                            .append(this.handleValue(DCF3, beHeightC))
                            .append(EOL);
                }

                count++;
            }

            this.prepContent.append(SECTION_TRIANGLES).append(' ').append(count).append(EOL);
            this.prepContent.append(tmpContent);
            this.prepContent.append(EOL);
        } finally {
            result.close();
        }

        LOG.info("TRIANGLES HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveCurves(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING CURVES");

        ResultSet result = null;

        try {
            result = stmt.executeQuery(
                    " select id, identifier"
                            + " from geocpm_curve"
                            + " where geocpm_curve.geocpm_configuration_id = "
                            + this.configId
                            + " order by identifier");

            BigDecimal t;
            BigDecimal value;

            final StringBuilder tmpContent = new StringBuilder(1000);
            final ArrayList<String> ids = new ArrayList<String>();
            final ArrayList<String> identifiers = new ArrayList<String>();

            while (result.next()) {
                ids.add(this.handleValue(result.getString("id")));
                identifiers.add(this.handleValue(result.getString("identifier")));
            }

            final int numCurves = ids.size();
            for (int i = 0; i < numCurves; i++) {
                tmpContent.append(identifiers.get(i));

                result = stmt.executeQuery(
                        " select t, value"
                                + " from geocpm_curve_value"
                                + " where geocpm_curve_id = "
                                + ids.get(i));

                while (result.next()) {
                    t = result.getBigDecimal("t");
                    value = result.getBigDecimal("value");

                    tmpContent.append(FIELD_SEP)
                            .append(this.handleValue(DCF3, t))
                            .append(FIELD_SEP)
                            .append(this.handleValue(DCF3, value));
                }

                tmpContent.append(EOL);
            }

            tmpContent.append(EOL);

            this.prepContent.append(SECTION_CURVES).append(' ').append(numCurves).append(EOL);
            this.prepContent.append(tmpContent);
        } finally {
            if (result != null) {
                result.close();
            }
        }

        LOG.info("CURVES HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveSourceDrains(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING SOURCE-DRAINS");

        ResultSet result = null;
        int count = 0;
        try {
            result = stmt.executeQuery("select sd.identifier, t.index, sd.max_capacity, c.identifier, sd.max_capacity"
                            + " from geocpm_source_drain sd, geocpm_curve c, geocpm_triangle t"
                            + " where sd.geocpm_configuration_id = " + this.configId
                            + " and   sd.geocpm_triangle_id = t.id"
                            + " and   sd.geocpm_curve_id    = c.id");

            String sourceDrainIdentifier;
            String index;
            BigDecimal capacity;
            String curveIdentifier;

            final StringBuilder tmpContent = new StringBuilder(this.prepContent.capacity() / 3);

            while (result.next()) {
                sourceDrainIdentifier = this.handleValue(result.getString(1));
                index = this.handleValue(result.getString(2));
                capacity = result.getBigDecimal(3);
                curveIdentifier = this.handleValue(result.getString(4));

                tmpContent.append(sourceDrainIdentifier)
                        .append(FIELD_SEP)
                        .append(index)
                        .append(FIELD_SEP)
                        .append(this.handleValue(DCF3, capacity))
                        .append(FIELD_SEP)
                        .append(curveIdentifier)
                        .append(EOL);

                count++;
            }

            this.prepContent.append(SECTION_SOURCE_DRAIN).append(' ').append(count).append(EOL);
            this.prepContent.append(tmpContent);
            this.prepContent.append(EOL);
        } finally {
            result.close();
        }

        LOG.info("SOURCE_DRAINS HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveManholes(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING MANHOLES");

        ResultSet result = null;

        try {
            result = stmt.executeQuery(
                    "select id, internal_id,  cap_height, entry_profile, loss_overfall, loss_emersion, length_emersion, name"
                            + " from geocpm_manhole"
                            + " where geocpm_configuration_id = "
                            + this.configId);

            StringBuilder tmpContent;

            final ArrayList<String> manholeIds = new ArrayList<String>();
            final ArrayList<StringBuilder> halfRecs = new ArrayList<StringBuilder>();

            while (result.next()) {
                manholeIds.add(this.handleValue(result.getString(1))); // id

                tmpContent = new StringBuilder(50);

                tmpContent.append(result.getString(2)).append(FIELD_SEP);                             // internal id
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(3))).append(FIELD_SEP); // cap_height
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(4))).append(FIELD_SEP); // entry_profile
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(5))).append(FIELD_SEP); // loss_overfall
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(6))).append(FIELD_SEP); // loss_emersion
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(7))).append(FIELD_SEP); // length_emersion
                tmpContent.append(this.handleValue(result.getString(8))).append(EOL);                 // length_emersion

                halfRecs.add(tmpContent);
            }

            final StringBuilder tmpFinalContent = new StringBuilder(1000);
            int numTriangles;
            final int numManholes = manholeIds.size();
            for (int i = 0; i < numManholes; i++) {
                result = stmt.executeQuery(
                        "select t.index"
                                + " from   geocpm_jt_manhole_triangle, geocpm_triangle t"
                                + " where  geocpm_manhole_id  = "
                                + manholeIds.get(i)
                                + " and    geocpm_triangle_id = t.id");

                numTriangles = 0;
                tmpContent = new StringBuilder(100);
                while (result.next()) {
                    tmpContent.append(this.handleValue(result.getString(1))).append(FIELD_SEP);
                    numTriangles++;
                }

                tmpFinalContent.append(numTriangles).append(FIELD_SEP);
                tmpFinalContent.append(tmpContent);
                tmpFinalContent.append(halfRecs.get(i));
            }

            this.prepContent.append(SECTION_MANHOLES).append(' ').append(numManholes).append(EOL);
            this.prepContent.append(tmpFinalContent);
            this.prepContent.append(EOL);
        } finally {
            if (result != null) {
                result.close();
            }
        }

        LOG.info("MANHOLES HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveMarked(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING MARKED TRIANGLES");

        int count = 0;
        ResultSet result = null;
        try {
            result = stmt.executeQuery("select index"
                            + " from geocpm_triangle"
                            + " where geocpm_configuration_id = " + this.configId
                            + " and marked = true"
                            + " order by index");

            final StringBuilder tmpContent = new StringBuilder(1000);

            while (result.next()) {
                tmpContent.append(result.getString(1)).append(EOL);
                count++;
            }

            this.prepContent.append(SECTION_MARKED).append(' ').append(count).append(EOL);
            this.prepContent.append(tmpContent);
            this.prepContent.append(EOL);
        } finally {
            result.close();
        }

        LOG.info("MARKED TRIANGLES HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stmt  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void retrieveBKs(final Statement stmt) throws SQLException {
        LOG.info("START RETRIEVING BREAKING EDGES");

        ResultSet result = null;

        try {
            result = stmt.executeQuery(
                    "select id, index, type, height, triangle_count_high, triangle_count_low"
                            + " from geocpm_breaking_edge"
                            + " where geocpm_configuration_id = "
                            + this.configId
                            + " order by id");

            StringBuilder tmpContent;

            final ArrayList<String> bkIds = new ArrayList<String>();
            final ArrayList<StringBuilder> halfRecs = new ArrayList<StringBuilder>();

            while (result.next()) {
                bkIds.add(this.handleValue(result.getString(1))); // id

                tmpContent = new StringBuilder(50);

                tmpContent.append(result.getString(2)).append(FIELD_SEP);                             // index
                tmpContent.append(result.getString(3)).append(FIELD_SEP);                             // type
                tmpContent.append(this.handleValue(DCF2, result.getBigDecimal(4))).append(FIELD_SEP); // height
                tmpContent.append(result.getString(5)).append(FIELD_SEP);                             // triangle_count_high
                tmpContent.append(result.getString(6));                                               // triangle_count_low
                halfRecs.add(tmpContent);
            }

            final StringBuilder tmpFinalContent = new StringBuilder(1000);
            final int numBKs = bkIds.size();
            for (int i = 0; i < numBKs; i++) {
                // Note: low and high triangles are determined by db insertion order (-> order by id)
                result = stmt.executeQuery(
                        "select t.index, b.orientation"
                                + " from geocpm_jt_breaking_edge_triangle b, geocpm_triangle t"
                                + " where b.geocpm_breaking_edge_id = "
                                + bkIds.get(i)
                                + " and   t.id = b.geocpm_triangle_id"
                                + " order by b.id");

                tmpContent = halfRecs.get(i);
                while (result.next()) {
                    tmpContent.append(FIELD_SEP).append(result.getString(1));                   // triangle index
                    tmpContent.append(FIELD_SEP).append(this.handleValue(result.getString(2))); // orientation
                }

                tmpFinalContent.append(tmpContent).append(EOL);
            }

            this.prepContent.append(SECTION_BK_CONNECT).append(' ').append(numBKs).append(EOL);
            this.prepContent.append(tmpFinalContent);
        } catch (final Exception e) {
            LOG.error("An error occurred while processing BREAKING EADGES information", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }

        LOG.info("BREAKING EDGES HAVE BEEN RETRIEVED SUCCESSFULLY");
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  SQLException    DOCUMENT ME!
     * @throws  IOException     DOCUMENT ME!
     * @throws  ParseException  DOCUMENT ME!
     */
    public void doExport() throws SQLException, IOException, ParseException {
        LOG.info("BEGIN EXPORT");

        Connection con = null;
        Statement stmt = null;

        try {
            con = DriverManager.getConnection(dbUrl, user, password);
            stmt = con.createStatement();

            this.retrieveConfigData(stmt);
            this.retrievePoints(stmt);
            this.retrieveTriangles(stmt);
            this.retrieveCurves(stmt);
            this.retrieveSourceDrains(stmt);
            this.retrieveManholes(stmt);
            this.retrieveMarked(stmt);
            // RAINCURVE is not handled in the exporter (see comments in class header)
            this.prepContent.append(SECTION_RAINCURVE).append(" 0").append(EOL).append(EOL);
            this.retrieveBKs(stmt);

            final FileOutputStream fout = new FileOutputStream(this.outFile);
            final BufferedOutputStream bOut = new BufferedOutputStream(fout);

            bOut.write(this.prepContent.toString().getBytes());
            bOut.close();

            LOG.info("EXPORT HAS BEEN COMPLETED SUCCESSFULLY FOR CONFIGURATION ID " + this.configId);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                stmt.close();
            }

            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void main(final String[] args) throws Exception {
        final Properties p = new Properties();
        p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender"); // NOI18N
        p.put("log4j.appender.Remote.remoteHost", "localhost");                // NOI18N
        p.put("log4j.appender.Remote.port", "4445");                           // NOI18N
        p.put("log4j.appender.Remote.locationInfo", "true");                   // NOI18N
        p.put("log4j.rootLogger", "ALL,Remote");                               // NOI18N
        PropertyConfigurator.configure(p);

        final GeoCPMExport exporter = new GeoCPMExport(
                1,
                new File("/tmp/GeoCPM_be_test.ein"),
                "postgres",
                "cismetz12",
                "jdbc:postgresql://192.168.100.12/wp6_db");

        exporter.doExport();
    }
}
