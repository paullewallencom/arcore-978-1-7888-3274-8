package com.olucurious.artreasurehunt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

/** A DialogFragment for the TextInput Dialog Box. */
public class TextInputDialogFragment extends DialogFragment {

  interface OkListener {
    void onOkPressed(String dialogValue);
  }

  private OkListener okListener;
  private EditText editTextInputField;

  /** Sets a listener that is invoked when the OK button on this dialog is pressed. */
  void setOkListener(OkListener okListener) {
    this.okListener = okListener;
  }

  /**
   * Creates a simple layout for the dialog. This contains a single user-editable text field
   */
  private LinearLayout getDialogLayout() {
    Context context = getContext();
    LinearLayout layout = new LinearLayout(context);
    editTextInputField = new EditText(context);
    editTextInputField.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
    editTextInputField.setLayoutParams(
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    editTextInputField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
    layout.addView(editTextInputField);
    layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    return layout;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder
            .setView(getDialogLayout())
            .setCancelable(false)
            .setTitle("Enter your name")
            .setPositiveButton(
                    "OK",
                    (dialog, which) -> {
                      Editable inputFieldText = editTextInputField.getText();
                      if (okListener != null && inputFieldText != null && inputFieldText.length() > 0) {
                        // Invoke the callback with the current checked item.
                        okListener.onOkPressed(inputFieldText.toString());
                      }
                    });
    return builder.create();
  }
}