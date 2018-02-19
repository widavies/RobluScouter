package com.cpjd.robluscouter.ui.dialogs;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * FastDialogBuilder is a extremely simple Yes/No/Neutral Dialog.
 * It works as a normal builder does. For the buttons, if the buttonText string is
 * "", then the button won't be added to the dialog at all.
 *
 * The dialog will ONLY close when one of the three buttons is pressed or the user exits
 * the app.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class FastDialogBuilder {
    /**
     * The title of the dialog
     */
    private String title = "";
    /**
     * The message of the dialog
     */
    private String message = "";
    /**
     * The text to set to the positive button, "" won't add the positive button
     */
    private String positiveButtonText = "";
    /**
     * The text to set to the neutral button, "" won't add the neutral button
     */
    private String neutralButtonText = "";
    /**
     * The text to set to the negative button, "" won't add the negative button
     */
    private String negativeButtonText = "";

    public interface FastDialogListener {
        void accepted();
        void denied();
        void neutral();
    }

    /**
     * The listener that will received all dialog events
     */
    private FastDialogListener listener;

    public FastDialogBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public FastDialogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }
    public FastDialogBuilder setPositiveButtonText(String text) {
        this.positiveButtonText = text;
        return this;
    }

    public FastDialogBuilder setNegativeButtonText(String text) {
        this.negativeButtonText = text;
        return this;
    }

    public FastDialogBuilder setNeutralButtonText(String text) {
        this.neutralButtonText = text;
        return this;
    }

    public FastDialogBuilder setFastDialogListener(FastDialogListener listener) {
        this.listener = listener;
        return this;
    }

    public void build(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        if(!positiveButtonText.equals("")) {
            builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.accepted();
                    dialog.dismiss();
                }
            });
        }

        if(!neutralButtonText.equals("")) {
            builder.setNeutralButton(neutralButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.neutral();
                    dialog.dismiss();
                }
            });
        }

        if(!negativeButtonText.equals("")) {
            builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.denied();
                    dialog.dismiss();
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }


}
