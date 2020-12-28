/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.olucurious.cloudanchor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.olucurious.cloudanchor.R;

/** A DialogFragment for the Resolve Dialog Box. */
public class ResolveDialogFragment extends DialogFragment {

  interface OkListener {
    void onOkPressed(String dialogValue);
  }

  private OkListener okListener;
  private EditText shortCodeField;

  /** Sets a listener that is invoked when the OK button on this dialog is pressed. */
  void setOkListener(OkListener okListener) {
    this.okListener = okListener;
  }

  /**
   * Creates a simple layout for the dialog. This contains a single user-editable text field whose
   * input type is retricted to numbers only, for simplicity.
   */
  private LinearLayout getDialogLayout() {
    Context context = getContext();
    LinearLayout layout = new LinearLayout(context);
    shortCodeField = new EditText(context);
    shortCodeField.setInputType(InputType.TYPE_CLASS_NUMBER);
    shortCodeField.setLayoutParams(
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    shortCodeField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
    layout.addView(shortCodeField);
    layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    return layout;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder
            .setView(getDialogLayout())
            .setTitle("Resolve Anchor")
            .setPositiveButton(
                    "OK",
                    (dialog, which) -> {
                      Editable shortCodeText = shortCodeField.getText();
                      if (okListener != null && shortCodeText != null && shortCodeText.length() > 0) {
                        // Invoke the callback with the current checked item.
                        okListener.onOkPressed(shortCodeText.toString());
                      }
                    })
            .setNegativeButton("Cancel", (dialog, which) -> {});
    return builder.create();
  }
}