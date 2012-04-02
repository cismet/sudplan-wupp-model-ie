/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.sudplan.wupp.geocpm.ie;

/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/

import org.apache.log4j.Logger;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   bfriedrich
 * @version  $Revision$, $Date$
 */
public final class ExportApp {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(ExportApp.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ExportApp object.
     */
    private ExportApp() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        try {
            final Properties p = new Properties();
            p.put("log4j.appender.Console", "org.apache.log4j.ConsoleAppender");
            p.put("log4j.appender.Console.layout", "org.apache.log4j.TTCCLayout");
            p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender");
            p.put("log4j.appender.Remote.remoteHost", "localhost");
            p.put("log4j.appender.Remote.port", "4445");
            p.put("log4j.appender.Remote.locationInfo", "true");
            p.put("log4j.rootLogger", "ALL,Console,Remote");
            org.apache.log4j.PropertyConfigurator.configure(p);

            LOG.info("start export with arguments " + Arrays.toString(args));

            final String configId = args[0];
            final String outFile = args[1];
            final String dbUrl = args[2];
            final String dbUser = args[3];
            final String dbPwd = args[4];
            final String rainEventId = args[5];

            Class.forName("org.postgresql.Driver");

            final Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPwd);
            final Statement stmt = con.createStatement();

            final ResultSet raineventResult = stmt.executeQuery("select name, data, interval from rainevent where id = "
                            + rainEventId);

            if (raineventResult.next()) {
                final String name = raineventResult.getString("name");
                LOG.info("processing rain event " + name);

                final String data = raineventResult.getString("data");
                final String[] dataSplits = data.split(":");

                final ArrayList<Double> rainValues = new ArrayList<Double>(dataSplits.length);
                for (int i = 0; i < dataSplits.length; i++) {
                    rainValues.add(Double.parseDouble(dataSplits[i]));
                }

                LOG.info("start export");
                final GeoCPMExport exporter = new GeoCPMExport(Integer.parseInt(configId),
                        new File(outFile),
                        dbUser,
                        dbPwd,
                        dbUrl);
                exporter.doExport();

                LOG.info("start DYNA generation");
                final int interval = raineventResult.getInt("interval");
                exporter.generateDYNA(interval, rainValues);

                LOG.info("Export has been finished successfully");
            } else {
                throw new RuntimeException("Couldn't find any data for RainEvent with id " + rainEventId);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
    }
}
