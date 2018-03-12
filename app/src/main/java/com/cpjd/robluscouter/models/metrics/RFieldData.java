package com.cpjd.robluscouter.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RFieldDiagram stores match data from TheBlueAlliance.com, this metric is always uneditable
 *
 * This metric actually isn't processed by Roblu Scouter, and is actually stripped of data when it's uploaded from
 * Roblu Master
 *
 * @author Will Davies
 * @since 4.4.0
 * @version 1
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RFieldData")
public class RFieldData extends RMetric {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    /**
     * The string is the metric name, the ArrayList stores 2 metrics (red first, blue second)
     * representing the match data.
     */
    private LinkedHashMap<String, ArrayList<RMetric>> data;

    public RFieldData() {}

    public RFieldData(int ID, String title) {
        super(ID, title);
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Field data";
    }

    @Override
    public RMetric clone() {
        RFieldData fieldData = new RFieldData(ID, title);
        fieldData.setData(data);
        return fieldData;

    }

}
