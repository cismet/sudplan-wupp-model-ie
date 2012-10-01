/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.sudplan.geoserver;

import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;

/**
 * A layer encoder which is aware of the 'path' element, which specifies the WMS path of the layer.
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class GSPathAwareLayerEncoder extends GSLayerEncoder {

    //~ Methods ----------------------------------------------------------------

    /**
     * Specify the WMS path of the layer.
     *
     * @param  path  The WMS path.
     */
    public void setPath(final String path) {
        if ((path != null) && !path.trim().isEmpty()) {
            set("path", path);
        }
    }
}
