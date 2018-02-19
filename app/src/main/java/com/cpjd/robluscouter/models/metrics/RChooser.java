package com.cpjd.robluscouter.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Represents a list of items from which only one item can be selected
 * @see com.cpjd.robluscouter.models.metrics.RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RChooser")
public class RChooser extends RMetric {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * Represents a list of possible items that can be selected
     */
    @NonNull
    private String[] values;
    /**
     * Represents the index of values[] which contains the currently selected item
     */
    private int selectedIndex;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RChooser() {}

    /**
     * Instantiates a chooser object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param values String[] containing possible item selections
     * @param selectedIndex the index of the currently selected value within values[]
     */
    private RChooser(int ID, String title, String[] values, int selectedIndex) {
        super(ID, title);
        this.values = values;
        this.selectedIndex = selectedIndex;
    }

    @Override
    public String getFormDescriptor() {
        StringBuilder descriptor = new StringBuilder("Type: Chooser\nItems: ");
        for(String s : values) descriptor.append(s);
        descriptor.append(" Default value index: ").append(selectedIndex);
        return descriptor.toString();
    }

    @Override
    public RMetric clone() {
        RChooser chooser = new RChooser(ID, title, values, selectedIndex);
        chooser.setRequired(required);
        return chooser;
    }
}

