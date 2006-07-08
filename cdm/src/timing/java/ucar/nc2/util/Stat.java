// $Id: NetworkUtils.java,v 1.5 2006/02/13 19:51:37 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.util;

import java.util.TreeMap;
import java.util.Iterator;

/**
 * Statistics utilities.
 *
 * @author caron
 * @version $Revision: 1.5 $ $Date: 2006/02/13 19:51:37 $
 */
public class Stat {
  private TreeMap store = new TreeMap();

  public Stat() {
  }

  public void avg( String what, long val) {
     if (store.containsKey( what)) {
       Node n = (Node) store.get(what);
       n.count++;
       n.accum += val;
     } else {
       Node n = new Node();
       n.count = 1;
       n.accum = val;
       store.put(what, n);
     }
  }

  public void print() {
    Iterator iter = store.keySet().iterator();
    while (iter.hasNext()) {
      String what = (String) iter.next();
      Node n = (Node) store.get(what);

      int avg = (int) (n.accum / n.count);
      System.out.println(what+ " "+n.accum+ "total msec "+avg+ "avg msec "+" count= "+n.count);
    }
    System.out.println("\n");

  }

  class Node {
    int   count;
    double accum;
  }
}