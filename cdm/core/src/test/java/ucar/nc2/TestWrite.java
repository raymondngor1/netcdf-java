/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test nc2 write JUnit framework.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWrite {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private boolean show = false;
  private static String writerLocation;

  @BeforeClass
  public static void setupClass() throws IOException {
    writerLocation = tempFolder.newFile().getAbsolutePath();
  }

  // This test must run before testNC3ReadExisting and testNC3WriteExisting, as those tests depend on the file that
  // this one creates. JUnit provides limited support for test ordering (http://stackoverflow.com/questions/3693626)
  // and in order to get it to work, this method's name must be lexicographically-less than the other 2.
  @Test
  public void firstTestNC3Write() throws IOException {
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, writerLocation, null);

    // add dimensions
    Dimension latDim = writer.addDimension(null, "lat", 64);
    Dimension lonDim = writer.addDimension(null, "lon", 128);

    // add Variable double temperature(lat,lon)
    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(latDim);
    dims.add(lonDim);
    Variable t = writer.addVariable(null, "temperature", DataType.DOUBLE, dims);
    t.addAttribute(new Attribute("units", "K")); // add a 1D attribute of length 3
    Array data = Array.factory(DataType.INT, new int[] {3}, new int[] {1, 2, 3});
    t.addAttribute(new Attribute("scale", data));

    // add a string-valued variable: char svar(80)
    Dimension svar_len = writer.addDimension(null, "svar_len", 80);
    writer.addVariable(null, "svar", DataType.CHAR, "svar_len");
    writer.addVariable(null, "svar2", DataType.CHAR, "svar_len");

    // add a 2D string-valued variable: char names(names, 80)
    Dimension names = writer.addDimension(null, "names", 3);
    Variable namesVar = writer.addVariable(null, "names", DataType.CHAR, "names svar_len");
    Variable names2Var = writer.addVariable(null, "names2", DataType.CHAR, "names svar_len");

    // add a scalar variable
    Variable scalarVar = writer.addVariable(null, "scalar", DataType.DOUBLE, new ArrayList<>());

    // signed byte
    Variable bVar = writer.addVariable(null, "bvar", DataType.BYTE, "lat");

    // add global attributes
    writer.addGroupAttribute(null, new Attribute("yo", "face"));
    writer.addGroupAttribute(null, new Attribute("versionD", 1.2));
    writer.addGroupAttribute(null, new Attribute("versionF", (float) 1.2));
    writer.addGroupAttribute(null, new Attribute("versionI", 1));
    writer.addGroupAttribute(null, new Attribute("versionS", (short) 2));
    writer.addGroupAttribute(null, new Attribute("versionB", (byte) 3));

    // test some errors
    try {
      Array bad = Array.makeObjectArray(DataType.OBJECT, ArrayList.class, new int[] {1}, null);
      writer.addGroupAttribute(null, new Attribute("versionB", bad));
      assert (false);
    } catch (IllegalArgumentException e) {
      assert (true);
    }

    // create the file
    try {
      writer.create();
    } catch (IOException e) {
      System.err.printf("ERROR creating file %s%n%s", writerLocation, e.getMessage());
    }
    // writer.close();

    // write some data
    ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
    int i, j;
    Index ima = A.getIndex();
    // write
    for (i = 0; i < latDim.getLength(); i++) {
      for (j = 0; j < lonDim.getLength(); j++) {
        A.setDouble(ima.set(i, j), (double) (i * 1000000 + j * 1000));
      }
    }

    int[] origin = new int[2];
    Variable v = writer.findVariable("temperature");
    try {
      writer.write(v, origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing file");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write char variable
    int[] origin1 = new int[1];
    ArrayChar ac = new ArrayChar.D1(svar_len.getLength());
    ima = ac.getIndex();
    String val = "Testing 1-2-3";
    for (j = 0; j < val.length(); j++)
      ac.setChar(ima.set(j), val.charAt(j));

    v = writer.findVariable("svar");
    try {
      writer.write(v, origin1, ac);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable
    ArrayByte.D1 barray = new ArrayByte.D1(latDim.getLength(), false);
    int start = -latDim.getLength() / 2;
    for (j = 0; j < latDim.getLength(); j++)
      barray.setByte(j, (byte) (start + j));

    v = writer.findVariable("bvar");
    try {
      writer.write(v, barray);
    } catch (IOException e) {
      System.err.println("ERROR writing bvar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable as String
    try {
      ArrayChar ac2 = new ArrayChar.D1(svar_len.getLength());
      ac2.setString("Two pairs of ladies stockings!");
      v = writer.findVariable("svar2");
      writer.write(v, origin1, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar2");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    try {
      ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
      ima = ac2.getIndex();
      ac2.setString(ima.set(0), "No pairs of ladies stockings!");
      ac2.setString(ima.set(1), "One pair of ladies stockings!");
      ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
      v = writer.findVariable("names");
      writer.write(v, origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar3");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    try {
      ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
      ac2.setString(0, "0 pairs of ladies stockings!");
      ac2.setString(1, "1 pair of ladies stockings!");
      ac2.setString(2, "2 pairs of ladies stockings!");
      v = writer.findVariable("names2");
      writer.write(v, origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar4");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write scalar data
    // write String array
    try {
      ArrayDouble.D0 datas = new ArrayDouble.D0();
      datas.set(222.333);
      v = writer.findVariable("scalar");
      writer.write(v, datas);
    } catch (IOException e) {
      System.err.println("ERROR writing scalar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    if (show)
      System.out.println("ncfile = " + writer.getNetcdfFile());

    try {
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    //////////////////////////////////////////////////////////////////////
    // test reading without closing the file
    NetcdfFile ncfile = writer.getNetcdfFile();

    // read entire array
    Variable temp = ncfile.findVariable("temperature");
    assert (null != temp);

    Array tA = temp.read();
    assert (tA.getRank() == 2);

    ima = tA.getIndex();
    int[] shape = tA.getShape();

    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
      }
    }

    // read part of array
    int[] origin2 = new int[2];
    int[] shape2 = new int[2];
    shape2[0] = 1;
    shape2[1] = temp.getShape()[1];
    try {
      tA = temp.read(origin2, shape2);
    } catch (InvalidRangeException e) {
      System.err.println("ERROR reading file " + e);
      assert (false);
      return;
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }
    assert (tA.getRank() == 2);

    for (j = 0; j < shape2[1]; j++) {
      assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
    }

    // rank reduction
    Array Areduce = tA.reduce();
    Index ima2 = Areduce.getIndex();
    assert (Areduce.getRank() == 1);

    for (j = 0; j < shape2[1]; j++) {
      assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
    }

    // read char variable
    Variable c = null;
    assert (null != (c = ncfile.findVariable("svar")));
    try {
      tA = c.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar achar = (ArrayChar) tA;
    String sval = achar.getString(achar.getIndex());
    assert sval.equals("Testing 1-2-3") : sval;
    // System.out.println( "val = "+ val);

    // read char variable 2
    Variable c2 = null;
    assert (null != (c2 = ncfile.findVariable("svar2")));
    try {
      tA = c2.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac2 = (ArrayChar) tA;
    assert (ac2.getString().equals("Two pairs of ladies stockings!"));

    // read String Array
    Variable c3 = null;
    assert (null != (c3 = ncfile.findVariable("names")));
    try {
      tA = c3.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac3 = (ArrayChar) tA;
    ima = ac3.getIndex();

    assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
    assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
    assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

    // read String Array - 2
    Variable c4 = null;
    assert (null != (c4 = ncfile.findVariable("names2")));
    try {
      tA = c4.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac4 = (ArrayChar) tA;

    assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
    assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
    assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));

    /////////////////////////////////////////////////////////////////////
    // all done
    try {
      ncfile.close();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }
  }

  @Test
  public void testNC3WriteExisting() throws IOException {
    NetcdfFileWriter writer = NetcdfFileWriter.openExisting(writerLocation);

    Variable v = writer.findVariable("temperature");
    int[] shape = v.getShape();
    ArrayDouble A = new ArrayDouble.D2(shape[0], shape[1]);
    int i, j;
    Index ima = A.getIndex();
    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        A.setDouble(ima.set(i, j), (double) (i * 1000000 + j * 1000));
      }
    }

    int[] origin = new int[2];
    try {
      writer.write(v, origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing file");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write char variable
    v = writer.findVariable("svar");
    shape = v.getShape();
    int[] origin1 = new int[1];
    ArrayChar ac = new ArrayChar.D1(shape[0]);
    ima = ac.getIndex();
    String val = "Testing 1-2-3";
    for (j = 0; j < val.length(); j++)
      ac.setChar(ima.set(j), val.charAt(j));

    try {
      writer.write(v, origin1, ac);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable
    v = writer.findVariable("bvar");
    shape = v.getShape();
    int len = shape[0];
    ArrayByte.D1 barray = new ArrayByte.D1(len, false);
    int start = -len / 2;
    for (j = 0; j < len; j++)
      barray.setByte(j, (byte) (start + j));

    try {
      writer.write(v, barray);
    } catch (IOException e) {
      System.err.println("ERROR writing bvar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable as String
    v = writer.findVariable("svar2");
    shape = v.getShape();
    len = shape[0];
    try {
      ArrayChar ac2 = new ArrayChar.D1(len);
      ac2.setString("Two pairs of ladies stockings!");
      writer.write(v, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar2");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    v = writer.findVariable("names");
    shape = v.getShape();
    try {
      ArrayChar ac2 = new ArrayChar.D2(shape[0], shape[1]);
      ima = ac2.getIndex();
      ac2.setString(ima.set(0), "No pairs of ladies stockings!");
      ac2.setString(ima.set(1), "One pair of ladies stockings!");
      ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
      writer.write(v, origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar3");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    try {
      ArrayChar ac2 = new ArrayChar.D2(shape[0], shape[1]);
      ac2.setString(0, "0 pairs of ladies stockings!");
      ac2.setString(1, "1 pair of ladies stockings!");
      ac2.setString(2, "2 pairs of ladies stockings!");
      v = writer.findVariable("names2");
      writer.write(v, origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar4");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write scalar data
    try {
      ArrayDouble.D0 datas = new ArrayDouble.D0();
      datas.set(222.333);
      v = writer.findVariable("scalar");
      writer.write(v, datas);
    } catch (IOException e) {
      System.err.println("ERROR writing scalar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    if (show)
      System.out.println("ncfile = " + writer.getNetcdfFile());

    try {
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }
  }

  // test reading after closing the file
  @Test
  public void testNC3ReadExisting() throws IOException {
    NetcdfFile ncfile = NetcdfFiles.open(writerLocation);

    // read entire array
    Variable temp = ncfile.findVariable("temperature");
    assert (null != temp);

    Array tA = temp.read();
    assert (tA.getRank() == 2);

    Index ima = tA.getIndex();
    int[] shape = tA.getShape();

    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
      }
    }

    // read part of array
    int[] origin2 = new int[2];
    int[] shape2 = new int[2];
    shape2[0] = 1;
    shape2[1] = temp.getShape()[1];
    try {
      tA = temp.read(origin2, shape2);
    } catch (InvalidRangeException e) {
      System.err.println("ERROR reading file " + e);
      assert (false);
      return;
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }
    assert (tA.getRank() == 2);

    for (int j = 0; j < shape2[1]; j++) {
      assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
    }

    // rank reduction
    Array Areduce = tA.reduce();
    Index ima2 = Areduce.getIndex();
    assert (Areduce.getRank() == 1);

    for (int j = 0; j < shape2[1]; j++) {
      assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
    }

    // read char variable
    Variable c = null;
    assert (null != (c = ncfile.findVariable("svar")));
    try {
      tA = c.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar achar = (ArrayChar) tA;
    String sval = achar.getString(achar.getIndex());
    assert sval.equals("Testing 1-2-3") : sval;
    // System.out.println( "val = "+ val);

    // read char variable 2
    Variable c2 = null;
    assert (null != (c2 = ncfile.findVariable("svar2")));
    try {
      tA = c2.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac2 = (ArrayChar) tA;
    assert (ac2.getString().equals("Two pairs of ladies stockings!"));

    // read String Array
    Variable c3 = null;
    assert (null != (c3 = ncfile.findVariable("names")));
    try {
      tA = c3.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac3 = (ArrayChar) tA;
    ima = ac3.getIndex();

    assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
    assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
    assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

    // read String Array - 2
    Variable c4 = null;
    assert (null != (c4 = ncfile.findVariable("names2")));
    try {
      tA = c4.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac4 = (ArrayChar) tA;
    ima = ac4.getIndex();

    assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
    assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
    assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));

    /////////////////////////////////////////////////////////////////////
    // all done
    try {
      ncfile.close();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }
  }

  @Test
  public void testWriteRecordOneAtaTime() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
      // define dimensions, including unlimited
      Dimension latDim = writer.addDimension(null, "lat", 3);
      Dimension lonDim = writer.addDimension(null, "lon", 4);
      Dimension timeDim = writer.addUnlimitedDimension("time");

      // define Variables
      Variable lat = writer.addVariable(null, "lat", DataType.FLOAT, "lat");
      lat.addAttribute(new Attribute("units", "degrees_north"));
      Variable lon = writer.addVariable(null, "lon", DataType.FLOAT, "lon");
      lon.addAttribute(new Attribute("units", "degrees_east"));
      Variable rh = writer.addVariable(null, "rh", DataType.INT, "time lat lon");
      rh.addAttribute(new Attribute("long_name", "relative humidity"));
      rh.addAttribute(new Attribute("units", "percent"));
      Variable t = writer.addVariable(null, "T", DataType.DOUBLE, "time lat lon");
      t.addAttribute(new Attribute("long_name", "surface temperature"));
      t.addAttribute(new Attribute("units", "degC"));
      Variable time = writer.addVariable(null, "time", DataType.INT, "time");
      time.addAttribute(new Attribute("units", "hours since 1990-01-01"));

      // create the file
      writer.create();

      // write out the non-record variables
      writer.write(lat, Array.makeFromJavaArray(new float[] {41, 40, 39}, false));
      writer.write(lon, Array.makeFromJavaArray(new float[] {-109, -107, -105, -103}, false));

      //// heres where we write the record variables
      // different ways to create the data arrays.
      // Note the outer dimension has shape 1, since we will write one record at a time
      ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength(), false);
      ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      Array timeData = Array.factory(DataType.INT, new int[] {1});
      Index ima = rhData.getIndex();

      int[] origin = new int[] {0, 0, 0};
      int[] time_origin = new int[] {0};

      // loop over each record
      for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
        // make up some data for this record, using different ways to fill the data arrays.
        timeData.setInt(timeData.getIndex(), timeIdx * 12);

        for (int latIdx = 0; latIdx < latDim.getLength(); latIdx++) {
          for (int lonIdx = 0; lonIdx < lonDim.getLength(); lonIdx++) {
            rhData.setInt(ima.set(0, latIdx, lonIdx), timeIdx * latIdx * lonIdx);
            tempData.set(0, latIdx, lonIdx, timeIdx * latIdx * lonIdx / 3.14159);
          }
        }
        // write the data out for one record
        // set the origin here
        time_origin[0] = timeIdx;
        origin[0] = timeIdx;
        writer.write(rh, origin, rhData);
        writer.write(t, origin, tempData);
        writer.write(time, time_origin, timeData);
      } // loop over record
    }
  }

  @Test
  public void testNC3WriteOld() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
      // define dimensions
      Dimension latDim = ncfile.addDimension("lat", 64);
      Dimension lonDim = ncfile.addDimension("lon", 128);

      // define Variables
      Variable tempVar = ncfile.addVariable("temperature", DataType.DOUBLE, "lat lon");
      ncfile.addVariableAttribute("temperature", "units", "K");

      Array data = Array.factory(DataType.INT, new int[] {3}, new int[] {1, 2, 3});
      ncfile.addVariableAttribute(tempVar, new Attribute("scale", data));
      ncfile.addVariableAttribute("temperature", "versionD", 1.2);
      ncfile.addVariableAttribute("temperature", "versionF", 1.2f);
      ncfile.addVariableAttribute("temperature", "versionI", 1);
      ncfile.addVariableAttribute("temperature", "versionS", (short) 2);
      ncfile.addVariableAttribute("temperature", "versionB", (byte) 3);

      ncfile.addVariableAttribute("temperature", "versionString", "1.2");

      // add string-valued variables
      Dimension svar_len = ncfile.addDimension("svar_len", 80);
      Variable sVar = ncfile.addVariable("svar", DataType.CHAR, "svar_len");
      Variable sVar2 = ncfile.addVariable("svar2", DataType.CHAR, "svar_len");

      // string array
      Dimension names = ncfile.addDimension("names", 3);

      Variable namesVar = ncfile.addVariable("names", DataType.CHAR, "names svar_len");
      Variable names2Var = ncfile.addVariable("names2", DataType.CHAR, "names svar_len");

      // how about a scalar variable?
      Variable scalarVar = ncfile.addVariable("scalar", DataType.DOUBLE, "");

      // signed byte
      Variable bVar = ncfile.addVariable("bvar", DataType.BYTE, "lat");

      // add global attributes
      ncfile.addGlobalAttribute("yo", "face");
      ncfile.addGlobalAttribute("versionD", 1.2);
      ncfile.addGlobalAttribute("versionF", 1.2f);
      ncfile.addGlobalAttribute("versionI", 1);
      ncfile.addGlobalAttribute("versionS", (short) 2);
      ncfile.addGlobalAttribute("versionB", (byte) 3);

      // test some errors
      try {
        Array bad = Array.makeObjectArray(DataType.OBJECT, ArrayList.class, new int[] {1}, null);
        ncfile.addGroupAttribute(null, new Attribute("versionC", bad));
        assert (false);
      } catch (IllegalArgumentException e) {
        assert (true);
      }
      // test some errors
      try {
        Array bad = Array.makeObjectArray(DataType.OBJECT, ArrayList.class, new int[] {1}, null);
        ncfile.addGroupAttribute(null, new Attribute("versionC", bad));
        assert (false);
      } catch (IllegalArgumentException e) {
        assert (true);
      }

      // create the file
      try {
        ncfile.create();
      } catch (IOException e) {
        System.err.println("ERROR creating file " + ncfile.getNetcdfFile().getLocation() + "\n" + e);
        assert (false);
      }

      // write some data
      ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      int i, j;
      Index ima = A.getIndex();
      // write
      for (i = 0; i < latDim.getLength(); i++) {
        for (j = 0; j < lonDim.getLength(); j++) {
          A.setDouble(ima.set(i, j), (double) (i * 1000000 + j * 1000));
        }
      }

      int[] origin = new int[2];
      try {
        ncfile.write("temperature", origin, A);
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      int[] origin1 = new int[1];
      ArrayChar ac = new ArrayChar.D1(svar_len.getLength());
      ima = ac.getIndex();
      String val = "Testing 1-2-3";
      for (j = 0; j < val.length(); j++) {
        ac.setChar(ima.set(j), val.charAt(j));
      }

      try {
        ncfile.write("svar", origin1, ac);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      try {
        ncfile.write(sVar, origin1, ac);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      ArrayByte.D1 barray = new ArrayByte.D1(latDim.getLength(), false);
      int start = -latDim.getLength() / 2;
      for (j = 0; j < latDim.getLength(); j++)
        barray.setByte(j, (byte) (start + j));

      try {
        ncfile.write(bVar, barray);
      } catch (IOException e) {
        System.err.println("ERROR writing bvar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable as String
      try {
        ArrayChar ac2 = new ArrayChar.D1(svar_len.getLength());
        ac2.setString("Two pairs of ladies stockings!");
        ncfile.write(sVar2, origin1, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar2");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ima = ac2.getIndex();
        ac2.setString(ima.set(0), "No pairs of ladies stockings!");
        ac2.setString(ima.set(1), "One pair of ladies stockings!");
        ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
        ncfile.write("names", origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar3");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ima = ac2.getIndex();
        ac2.setString(ima.set(0), "No pairs of ladies stockings!");
        ac2.setString(ima.set(1), "One pair of ladies stockings!");
        ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
        ncfile.write(namesVar, origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar3");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ac2.setString(0, "0 pairs of ladies stockings!");
        ac2.setString(1, "1 pair of ladies stockings!");
        ac2.setString(2, "2 pairs of ladies stockings!");
        ncfile.write("names2", origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar4");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ac2.setString(0, "0 pairs of ladies stockings!");
        ac2.setString(1, "1 pair of ladies stockings!");
        ac2.setString(2, "2 pairs of ladies stockings!");
        ncfile.write(names2Var, origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar4");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write scalar data
      // write String array
      try {
        ArrayDouble.D0 datas = new ArrayDouble.D0();
        datas.set(222.333);
        ncfile.write(scalarVar, datas);
      } catch (IOException e) {
        System.err.println("ERROR writing scalar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      if (show) {
        System.out.println("ncfile = " + ncfile);
      }


      //////////////////////////////////////////////////////////////////////
      // test reading without closing the file

      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      Array tA = temp.read();
      assert (tA.getRank() == 2);

      ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        tA = temp.read(origin2, shape2);
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assert (false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (tA.getRank() == 2);

      for (j = 0; j < shape2[1]; j++) {
        assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
      }

      // rank reduction
      Array Areduce = tA.reduce();
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (j = 0; j < shape2[1]; j++) {
        assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
      }

      // read char variable
      Variable c = null;
      assert (null != (c = ncfile.findVariable("svar")));
      try {
        tA = c.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar achar = (ArrayChar) tA;
      String sval = achar.getString(ac.getIndex());
      assert sval.equals("Testing 1-2-3") : sval;
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = null;
      assert (null != (c2 = ncfile.findVariable("svar2")));
      try {
        tA = c2.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac2 = (ArrayChar) tA;
      assert (ac2.getString().equals("Two pairs of ladies stockings!"));

      // read String Array
      Variable c3 = null;
      assert (null != (c3 = ncfile.findVariable("names")));
      try {
        tA = c3.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac3 = (ArrayChar) tA;
      ima = ac3.getIndex();

      assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
      assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
      assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

      // read String Array - 2
      Variable c4 = null;
      assert (null != (c4 = ncfile.findVariable("names2")));
      try {
        tA = c4.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac4 = (ArrayChar) tA;

      assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
      assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
      assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));
    }
  }

  // fix for bug introduced 2/9/10, reported by Christian Ward-Garrison cwardgar@usgs.gov
  @Test
  public void testRecordSizeBug() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    int size = 10;
    Array timeDataAll = Array.factory(DataType.INT, new int[] {size});

    try (NetcdfFileWriter ncWriteable = NetcdfFileWriter.createNew(filename, false)) {
      Dimension timeDim = ncWriteable.addUnlimitedDimension("time");
      ncWriteable.addVariable("time", DataType.INT, "time");
      ncWriteable.addVariableAttribute("time", "units", "hours since 1990-01-01");
      ncWriteable.create();

      IndexIterator iter = timeDataAll.getIndexIterator();
      Array timeData = Array.factory(DataType.INT, new int[] {1});
      int[] time_origin = new int[] {0};

      for (int time = 0; time < size; time++) {
        int val = time * 12;
        iter.setIntNext(val);
        timeData.setInt(timeData.getIndex(), val);
        time_origin[0] = time;
        ncWriteable.write("time", time_origin, timeData);
      }
    }

    try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
      Array result = ncFile.readSection("time");
      Assert.assertEquals("0 12 24 36 48 60 72 84 96 108", result.toString().trim());
    }
  }
}
