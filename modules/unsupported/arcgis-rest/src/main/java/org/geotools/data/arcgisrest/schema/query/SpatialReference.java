/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */

package org.geotools.data.arcgisrest.schema.query;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class SpatialReference {

    /** (Required) */
    @SerializedName("wkid")
    @Expose
    private Integer wkid;
    /** (Required) */
    @SerializedName("latestWkid")
    @Expose
    private Integer latestWkid;

    @SerializedName("wkt")
    @Expose
    private String wkt;

    /** (Required) */
    public Integer getWkid() {
        return wkid;
    }

    /** (Required) */
    public void setWkid(Integer wkid) {
        this.wkid = wkid;
    }

    /** (Required) */
    public Integer getLatestWkid() {
        return latestWkid;
    }

    /** (Required) */
    public void setLatestWkid(Integer latestWkid) {
        this.latestWkid = latestWkid;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SpatialReference.class.getName())
                .append('@')
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append('[');
        sb.append("wkid");
        sb.append('=');
        sb.append(((this.wkid == null) ? "<null>" : this.wkid));
        sb.append(',');
        sb.append("latestWkid");
        sb.append('=');
        sb.append(((this.latestWkid == null) ? "<null>" : this.latestWkid));
        sb.append(',');
        sb.append("wkt");
        sb.append('=');
        sb.append(((this.wkt == null) ? "<null>" : this.wkt));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.wkid == null) ? 0 : this.wkid.hashCode()));
        result = ((result * 31) + ((this.wkt == null) ? 0 : this.wkt.hashCode()));
        result = ((result * 31) + ((this.latestWkid == null) ? 0 : this.latestWkid.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SpatialReference) == false) {
            return false;
        }
        SpatialReference rhs = ((SpatialReference) other);
        return Objects.equals(this.wkid, rhs.wkid)
                && Objects.equals(this.wkt, rhs.wkt)
                && Objects.equals(this.latestWkid, rhs.latestWkid);
    }
}
