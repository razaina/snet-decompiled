package com.google.android.gms.dynamic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.GooglePlayServicesUtilLight;
import com.google.android.gms.common.internal.ConnectionErrorMessages;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class DeferredLifecycleHelper<T extends LifecycleDelegate> {
    private static final int STATE_CREATED = 1;
    private static final int STATE_INFLATED = 0;
    private static final int STATE_RESUMED = 5;
    private static final int STATE_STARTED = 4;
    private static final int STATE_VIEW_CREATED = 2;
    private static final String TAG = "DeferredLifecycleHelper";
    private T mDelegate;
    private final OnDelegateCreatedListener<T> mDelegateCreationListener;
    private LinkedList<DeferredStateAction> mQueuedActions;
    private Bundle mSavedStateToProcess;

    private interface DeferredStateAction {
        int getState();

        void run(LifecycleDelegate lifecycleDelegate);
    }

    /* renamed from: com.google.android.gms.dynamic.DeferredLifecycleHelper.2 */
    class AnonymousClass2 implements DeferredStateAction {
        final /* synthetic */ Activity val$activity;
        final /* synthetic */ Bundle val$attrs;
        final /* synthetic */ Bundle val$savedInstanceState;

        AnonymousClass2(Activity activity, Bundle bundle, Bundle bundle2) {
            this.val$activity = activity;
            this.val$attrs = bundle;
            this.val$savedInstanceState = bundle2;
        }

        public int getState() {
            return DeferredLifecycleHelper.STATE_INFLATED;
        }

        public void run(LifecycleDelegate delegate) {
            DeferredLifecycleHelper.this.mDelegate.onInflate(this.val$activity, this.val$attrs, this.val$savedInstanceState);
        }
    }

    /* renamed from: com.google.android.gms.dynamic.DeferredLifecycleHelper.3 */
    class AnonymousClass3 implements DeferredStateAction {
        final /* synthetic */ Bundle val$savedInstanceState;

        AnonymousClass3(Bundle bundle) {
            this.val$savedInstanceState = bundle;
        }

        public int getState() {
            return DeferredLifecycleHelper.STATE_CREATED;
        }

        public void run(LifecycleDelegate delegate) {
            DeferredLifecycleHelper.this.mDelegate.onCreate(this.val$savedInstanceState);
        }
    }

    /* renamed from: com.google.android.gms.dynamic.DeferredLifecycleHelper.4 */
    class AnonymousClass4 implements DeferredStateAction {
        final /* synthetic */ ViewGroup val$container;
        final /* synthetic */ FrameLayout val$holder;
        final /* synthetic */ LayoutInflater val$inflater;
        final /* synthetic */ Bundle val$savedInstanceState;

        AnonymousClass4(FrameLayout frameLayout, LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            this.val$holder = frameLayout;
            this.val$inflater = layoutInflater;
            this.val$container = viewGroup;
            this.val$savedInstanceState = bundle;
        }

        public int getState() {
            return DeferredLifecycleHelper.STATE_VIEW_CREATED;
        }

        public void run(LifecycleDelegate delegate) {
            this.val$holder.removeAllViews();
            this.val$holder.addView(DeferredLifecycleHelper.this.mDelegate.onCreateView(this.val$inflater, this.val$container, this.val$savedInstanceState));
        }
    }

    /* renamed from: com.google.android.gms.dynamic.DeferredLifecycleHelper.5 */
    static class AnonymousClass5 implements OnClickListener {
        final /* synthetic */ Context val$context;
        final /* synthetic */ int val$errorCode;

        AnonymousClass5(Context context, int i) {
            this.val$context = context;
            this.val$errorCode = i;
        }

        public void onClick(View v) {
            this.val$context.startActivity(GooglePlayServicesUtil.getGooglePlayServicesAvailabilityRecoveryIntent(this.val$errorCode));
        }
    }

    protected abstract void createDelegate(OnDelegateCreatedListener<T> onDelegateCreatedListener);

    public DeferredLifecycleHelper() {
        this.mDelegateCreationListener = new OnDelegateCreatedListener<T>() {
            public void onDelegateCreated(T delegate) {
                DeferredLifecycleHelper.this.mDelegate = delegate;
                Iterator i$ = DeferredLifecycleHelper.this.mQueuedActions.iterator();
                while (i$.hasNext()) {
                    ((DeferredStateAction) i$.next()).run(DeferredLifecycleHelper.this.mDelegate);
                }
                DeferredLifecycleHelper.this.mQueuedActions.clear();
                DeferredLifecycleHelper.this.mSavedStateToProcess = null;
            }
        };
    }

    public T getDelegate() {
        return this.mDelegate;
    }

    private void popDeferredActions(int currentState) {
        while (!this.mQueuedActions.isEmpty() && ((DeferredStateAction) this.mQueuedActions.getLast()).getState() >= currentState) {
            this.mQueuedActions.removeLast();
        }
    }

    private void runOnDelegateReady(Bundle savedInstanceState, DeferredStateAction action) {
        if (this.mDelegate != null) {
            action.run(this.mDelegate);
            return;
        }
        if (this.mQueuedActions == null) {
            this.mQueuedActions = new LinkedList();
        }
        this.mQueuedActions.add(action);
        if (savedInstanceState != null) {
            if (this.mSavedStateToProcess == null) {
                this.mSavedStateToProcess = (Bundle) savedInstanceState.clone();
            } else {
                this.mSavedStateToProcess.putAll(savedInstanceState);
            }
        }
        createDelegate(this.mDelegateCreationListener);
    }

    public void onInflate(Activity activity, Bundle attrs, Bundle savedInstanceState) {
        runOnDelegateReady(savedInstanceState, new AnonymousClass2(activity, attrs, savedInstanceState));
    }

    public void onCreate(Bundle savedInstanceState) {
        runOnDelegateReady(savedInstanceState, new AnonymousClass3(savedInstanceState));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout holder = new FrameLayout(inflater.getContext());
        runOnDelegateReady(savedInstanceState, new AnonymousClass4(holder, inflater, container, savedInstanceState));
        if (this.mDelegate == null) {
            handleGooglePlayUnavailable(holder);
        }
        return holder;
    }

    protected void handleGooglePlayUnavailable(FrameLayout parent) {
        showGooglePlayUnavailableMessage(parent);
    }

    public static void showGooglePlayUnavailableMessage(FrameLayout parent) {
        Context context = parent.getContext();
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        String message = ConnectionErrorMessages.getErrorMessage(context, errorCode, GooglePlayServicesUtilLight.getAppName(context));
        String buttonMessage = ConnectionErrorMessages.getErrorDialogButtonMessage(context, errorCode);
        LinearLayout linearLayout = new LinearLayout(parent.getContext());
        linearLayout.setOrientation(STATE_CREATED);
        linearLayout.setLayoutParams(new LayoutParams(-2, -2));
        parent.addView(linearLayout);
        TextView text = new TextView(parent.getContext());
        text.setLayoutParams(new LayoutParams(-2, -2));
        text.setText(message);
        linearLayout.addView(text);
        if (buttonMessage != null) {
            Button button = new Button(context);
            button.setLayoutParams(new LayoutParams(-2, -2));
            button.setText(buttonMessage);
            linearLayout.addView(button);
            button.setOnClickListener(new AnonymousClass5(context, errorCode));
        }
    }

    public void onStart() {
        runOnDelegateReady(null, new DeferredStateAction() {
            public int getState() {
                return DeferredLifecycleHelper.STATE_STARTED;
            }

            public void run(LifecycleDelegate delegate) {
                DeferredLifecycleHelper.this.mDelegate.onStart();
            }
        });
    }

    public void onResume() {
        runOnDelegateReady(null, new DeferredStateAction() {
            public int getState() {
                return DeferredLifecycleHelper.STATE_RESUMED;
            }

            public void run(LifecycleDelegate delegate) {
                DeferredLifecycleHelper.this.mDelegate.onResume();
            }
        });
    }

    public void onPause() {
        if (this.mDelegate != null) {
            this.mDelegate.onPause();
        } else {
            popDeferredActions(STATE_RESUMED);
        }
    }

    public void onStop() {
        if (this.mDelegate != null) {
            this.mDelegate.onStop();
        } else {
            popDeferredActions(STATE_STARTED);
        }
    }

    public void onDestroyView() {
        if (this.mDelegate != null) {
            this.mDelegate.onDestroyView();
        } else {
            popDeferredActions(STATE_VIEW_CREATED);
        }
    }

    public void onDestroy() {
        if (this.mDelegate != null) {
            this.mDelegate.onDestroy();
        } else {
            popDeferredActions(STATE_CREATED);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (this.mDelegate != null) {
            this.mDelegate.onSaveInstanceState(outState);
        } else if (this.mSavedStateToProcess != null) {
            outState.putAll(this.mSavedStateToProcess);
        }
    }

    public void onLowMemory() {
        if (this.mDelegate != null) {
            this.mDelegate.onLowMemory();
        }
    }
}
