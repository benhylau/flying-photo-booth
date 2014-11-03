/*
 * Copyright (C) 2014 David Marques.
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
package com.groundupworks.wings.gcp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.dpsmarques.android.account.activity.AccountSelectionActivityHelper;
import com.dpsmarques.android.auth.GoogleOauthTokenObservable;
import com.dpsmarques.android.auth.activity.OperatorGoogleAuthenticationActivityController;
import com.github.dpsm.android.print.GoogleCloudPrint;
import com.github.dpsm.android.print.jackson.JacksonPrinterSearchResultOperator;
import com.github.dpsm.android.print.model.Printer;
import com.github.dpsm.android.print.model.PrinterSearchResult;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class GoogleCloudPrintPrinterSelectionActivity extends Activity implements AccountSelectionActivityHelper.AccountSelectionListener,
        OperatorGoogleAuthenticationActivityController.GoogleAuthenticationListener {

    private static final String GOOGLE_PRINT_SCOPE = "oauth2:https://www.googleapis.com/auth/cloudprint";

    private static final String[] ACCOUNT_TYPE = new String[]{"com.google"};

    private GoogleCloudPrint mGoogleCloudPrint;

    private static final int REQUEST_CODE_BASE = 1000;

    private final Action1<PrinterSearchResult> mUpdatePrinterListAction = new Action1<PrinterSearchResult>() {
        @Override
        public void call(final PrinterSearchResult response) {
            final List<Printer> printers = response.getPrinters();
            if (printers != null && printers.size() > 0) {
                mPrinterSpinner.setAdapter(new ArrayAdapter<Printer>(
                        GoogleCloudPrintPrinterSelectionActivity.this,
                        R.layout.gcp_activity_setup_spinner_item,
                        R.id.activity_main_spinner_item_text,
                        printers
                ));
            } else {
                Toast.makeText(getApplicationContext(), "No printers found.", Toast.LENGTH_LONG).show();
            }
        }
    };

    private final Action1<Throwable> mShowPrinterNotFoundAction = new Action1<Throwable>() {
        @Override
        public void call(final Throwable throwable) {
            Toast.makeText(GoogleCloudPrintPrinterSelectionActivity.this,
                "Searching printers failed :(", Toast.LENGTH_SHORT).show();
        }
    };

    private Spinner mPrinterSpinner;

    private Button mSelectPrinterButton;

    private AccountSelectionActivityHelper mAccountSelectionHelper;

    private Observable<String> mOauthObservable;

    private OperatorGoogleAuthenticationActivityController mAuthenticationHelper;
    private String mAccountSelected;
    private String mAuthenticationToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gcp_activity_setup);

        mAccountSelectionHelper = new AccountSelectionActivityHelper(this, REQUEST_CODE_BASE);
        mAuthenticationHelper = new OperatorGoogleAuthenticationActivityController(this, REQUEST_CODE_BASE + 100);

        mGoogleCloudPrint = new GoogleCloudPrint();
        mSelectPrinterButton = (Button) findViewById(R.id.activity_gcp_setup_printer_select);
        mPrinterSpinner = (Spinner) findViewById(R.id.activity_gcp_setup_printer_spinner);

        mSelectPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Printer selectedPrinter = (Printer) mPrinterSpinner.getSelectedItem();
                final Intent intent = new Intent();
                intent.putExtra("printer", selectedPrinter.getId());
                intent.putExtra("token", mAuthenticationToken);
                intent.putExtra("account", mAccountSelected);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    protected void onStart() {
        super.onStart();
        if (mOauthObservable == null) {
            mAccountSelectionHelper.selectUserAccount(ACCOUNT_TYPE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAccountSelectionHelper.handleActivityResult(requestCode, resultCode, data)) {
            return; // Handled by helper...
        }

        if (mAuthenticationHelper.handleActivityResult(requestCode, resultCode, data)) {
            return; // Handled by helper...
        }
    }

    @Override
    public void onAccountSelected(final String accountName) {
        mAccountSelected = accountName;
        mOauthObservable = GoogleOauthTokenObservable
            .create(this, accountName, GOOGLE_PRINT_SCOPE)
            .authenticateUsing(this, REQUEST_CODE_BASE)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        findPrinters();
    }

    @Override
    public void onAccountSelectionCanceled() {
        final Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onAuthenticationError(final Throwable throwable) {
        Toast.makeText(this, "Unknown authentication error!", Toast.LENGTH_SHORT).show();
        mOauthObservable = null;
    }

    @Override
    public void onAuthenticationSucceeded(final String token) {
        Toast.makeText(this, "Authenticated!", Toast.LENGTH_SHORT).show();
        mAuthenticationToken = token;
    }

    @Override
    public void onRetryAuthentication() {
        Toast.makeText(this, "Authentication temporarily failed!", Toast.LENGTH_SHORT).show();
    }

    private void findPrinters() {
        mOauthObservable.subscribe(new Action1<String>() {
            @Override
            public void call(final String token) {
                mGoogleCloudPrint.getPrinters(token)
                    .lift(new JacksonPrinterSearchResultOperator())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mUpdatePrinterListAction, mShowPrinterNotFoundAction);
            }
        });
    }
}
