/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.CDM;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;
import java.util.Formatter;

/**
 * Ndbc (National Data Buoy Center) Coards Convention (National Data Buoy Center)
 * 
 * @author caron
 * @since Apr 23, 2008
 */
public class NdbcCoards extends TableConfigurerImpl {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if (!ds.getRootGroup().findAttValueIgnoreCase(CDM.CONVENTIONS, "").equalsIgnoreCase("COARDS"))
      return false;

    String dataProvider = ds.getRootGroup().findAttValueIgnoreCase("data_provider", null);
    if (dataProvider == null)
      dataProvider = ds.getRootGroup().findAttValueIgnoreCase("institution", "");
    if (!dataProvider.contains("National Data Buoy Center"))
      return false;

    if (null == ds.getRootGroup().findAttValueIgnoreCase("station", null))
      return false;
    if (null == ds.getRootGroup().findAttValueIgnoreCase("location", null))
      return false;

    // if (ds.findVariable("lat") == null) return false;
    // if (ds.findVariable("lon") == null) return false;

    // DODS wont have record !!
    return ds.hasUnlimitedDimension();

  }

  /*
   * <!-- C:/data/dt2/station/ndbc.nc -->
   * <stationFeature>
   * <stationId>":station"</stationId>
   * <stationDesc>":description"</stationDesc>
   * <coordAxis type="lat">lat</coordAxis>
   * <coordAxis type="lon">lon</coordAxis>
   * <coordAxis type="height">0</coordAxis>
   * <table dim="time">
   * <coordAxis type="time">time</coordAxis>
   * </table>
   * </stationFeature>
   */

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    Dimension obsDim = ds.getUnlimitedDimension();
    if (obsDim == null) {
      CoordinateAxis axis = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
      if ((axis != null) && axis.isScalar())
        obsDim = axis.getDimension(0);
    }

    if (obsDim == null) {
      errlog.format("Must have an Observation dimension: unlimited dimension, or from Time Coordinate");
      return null;
    }
    boolean hasStruct = Evaluator.hasNetcdf3RecordStructure(ds);

    // wants a Point
    if ((wantFeatureType == FeatureType.POINT)) {
      TableConfig nt = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName());
      nt.structName = "record";
      nt.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      nt.featureType = FeatureType.POINT;
      CoordSysEvaluator.findCoords(nt, ds, null);
      return nt;
    }

    // otherwise, make it a Station
    TableConfig nt = new TableConfig(Table.Type.Top, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    nt.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);

    nt.stnId = ds.getRootGroup().findAttValueIgnoreCase("station", null);
    nt.stnDesc = ds.getRootGroup().findAttValueIgnoreCase("description", null);
    if (nt.stnDesc == null)
      nt.stnDesc = ds.getRootGroup().findAttValueIgnoreCase("comment", null);

    TableConfig obs = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName());
    obs.structName = "record";
    obs.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
    obs.dimName = obsDim.getShortName();
    obs.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    nt.addChild(obs);

    return nt;
  }
}
