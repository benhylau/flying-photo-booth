package com.groundupworks.wings.gcp;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.dpsmarques.android.account.AccountSelectionHelper;
import com.dpsmarques.android.account.activity.AccountSelectionActivityHelper;
import com.dpsmarques.android.auth.GoogleOauthTokenObservable;
import com.github.dpsm.android.print.GoogleCloudPrint;
import com.groundupworks.wings.AbstractWingsEndpoint;
import com.groundupworks.wings.IWingsNotification;
import com.groundupworks.wings.Wings;
import com.groundupworks.wings.core.ShareRequest;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import retrofit.client.Response;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by david.marques on 2014-10-30.
 */
public class GoogleCouldPrintEndpoint extends AbstractWingsEndpoint {

    private static final String[] ACCOUNT_TYPE = new String[]{"com.google"};

    private static final String TICKET = "{\n" +
        "  \"version\": \"1.0\",\n" +
        "  \"print\": {\n" +
        "    \"vendor_ticket_item\": [],\n" +
        "    \"color\": {\n" +
        "      \"type\": \"STANDARD_COLOR\"\n" +
        "    },\n" +
        "    \"media_size\": {\n" +
        "      \"width_microns\": 1,\n" +
        "      \"height_microns\": 1,\n" +
        "      \"is_continuous_feed\": false,\n" +
        "      \"vendor_id\" : \"EnvMonarch\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    private static final int REQUEST_CODE_BASE = 1000;

    private final GoogleCloudPrint mGoogleCloudPrint;

    private String mPrintIdentifier;

    private String mAccountName;

    private String mToken;

    public GoogleCouldPrintEndpoint() {
        mGoogleCloudPrint = new GoogleCloudPrint();
    }

    @Override
    public void startLinkRequest(final Activity activity, final Fragment fragment) {
        activity.startActivityForResult(new Intent(activity, GoogleCloudPrintPrinterSelectionActivity.class), REQUEST_CODE_BASE);
    }

    @Override
    public void unlink() {
        mToken = null;
    }

    @Override
    public boolean isLinked() {
        return !TextUtils.isEmpty(mToken);
    }

    @Override
    public void onResumeImpl() {

    }

    @Override
    public void onActivityResultImpl(final Activity activity, final Fragment fragment,
        final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_CODE_BASE) {
            if (resultCode == Activity.RESULT_OK) {
                mPrintIdentifier = data.getStringExtra("printer");
                mAccountName = data.getStringExtra("account");
                mToken = data.getStringExtra("token");
            }
        }
    }

    @Override
    public String getLinkedAccountName() {
        return mAccountName;
    }

    @Override
    public String getDestinationDescription() {
        return "Google Cloud Print";
    }

    @Override
    public IWingsNotification processShareRequests() {
        final Observable<ShareRequest> requestObservable = Observable
            .from(mDatabase.checkoutShareRequests(Wings.DESTINATION_GCP))
            .cache();


        final Observable<Response> responseObservable = requestObservable
            .map(new Func1<ShareRequest, File>() {
                @Override
                public File call(final ShareRequest shareRequest) {
                    return new File(shareRequest.getFilePath());
                }
            })
            .filter(new Func1<File, Boolean>() {
                @Override
                public Boolean call(final File file) {
                    return file.exists();
                }
            })
            .flatMap(new Func1<File, Observable<Response>>() {
                @Override
                public Observable<Response> call(final File file) {
                    return mGoogleCloudPrint.submitPrintJob(mToken, mPrintIdentifier,
                        file.getName(), TICKET, new TypedFile("image/jpeg", file));
                }
            });

        Observable<IWingsNotification> notificationObservable = Observable
            .zip(requestObservable, responseObservable, new Func2<ShareRequest, Response, Object>() {
            @Override
            public ShareRequest call(final ShareRequest shareRequest, final Response response) {
                if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                    mDatabase.markSuccessful(shareRequest.getId());
                } else {
                    mDatabase.markFailed(shareRequest.getId());
                }
                return shareRequest;
            }
        })
        .count()
        .map(new Func1<Integer, IWingsNotification>() {
            @Override
            public IWingsNotification call(final Integer count) {
                return new IWingsNotification() {

                    @Override
                    public int getId() {
                        return 0;
                    }

                    @Override
                    public String getTitle() {
                        return "Google Cloud Print";
                    }

                    @Override
                    public String getMessage() {
                        return String.format("Queued %d print jobs", count);
                    }

                    @Override
                    public String getTicker() {
                        return "Printing...";
                    }

                    @Override
                    public Intent getIntent() {
                        return new Intent();
                    }
                };
            }
        });

        IWingsNotification result = null;
        try {
            result = notificationObservable.toBlocking().last();
        } catch (Throwable t) {
            Log.e(getClass().getSimpleName(), "Failed to enqueue print jobs!", t);
        }
        return result;
    }
}
