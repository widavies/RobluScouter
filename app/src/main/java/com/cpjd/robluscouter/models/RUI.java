package com.cpjd.robluscouter.models;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;

import com.cpjd.robluscouter.R;

import java.io.Serializable;

import lombok.Data;

/**
 * Stores the UI settings for the app.
 *
 * @since 3.5.6
 * @author Will Davies
 */
@Data
public class RUI implements Serializable {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * primaryColor - toolbar color, some background colors (like floating action button)
     * accent - categories, text fields, highlight color
     * background - self explanatory
     * text - self explanatory
     * buttons - toolbar buttons, form buttons
     * cardColor - self explanatory
     * teamsRadius - how curvy to make the team cards
     * formRadius - how curvy to make the form ca rds
     * dialogDirection - the animation to be used with any given dialog
     * preset - ui looks already predefined by developer
     */

    private int primaryColor;
    private int accent;
    private int background;
    private int text;
    private int buttons;
    private int cardColor;
    private int teamsRadius;
    private int formRadius;
    private int dialogDirection;
    private int preset;

    public RUI() {
        primaryColor = -12627531;

        accent = -13784;
        cardColor = -12303292;
        background = -13619152;
        text = -1;
        buttons = -1;

        teamsRadius = 0;
        formRadius = 12;
        dialogDirection = 0;
        preset = 0;
    }

    /**
     * Gets the resource for the dialog direction variable
     * @return int representing the animation mode
     */
    public int getAnimation() {
        if(dialogDirection == 0) return R.style.dialog_animation;
        if(dialogDirection  == 1) return R.style.dialog_left_right;
        else return R.style.fade;
    }

    /**
     * Generate a color a few shades darker than the specified, used for the status bar color
     * @param color input color
     * @param factor the amount to darken it by
     * @return the darkened color
     */
    @ColorInt
    public static int darker(@ColorInt int color, @FloatRange(from = 0.0, to = 1.0) float factor) {
        return Color.argb(Color.alpha(color),
                Math.max((int) (Color.red(color) * factor), 0),
                Math.max((int) (Color.green(color) * factor), 0),
                Math.max((int) (Color.blue(color) * factor), 0)
        );
    }
}
