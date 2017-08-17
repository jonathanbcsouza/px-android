package com.mercadopago;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mercadopago.adapters.ReviewPaymentMethodsAdapter;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.preferences.DecorationPreference;
import com.mercadopago.presenters.ReviewPaymentMethodsPresenter;
import com.mercadopago.providers.ReviewPaymentMethodsProviderImpl;
import com.mercadopago.util.ErrorUtil;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.views.ReviewPaymentMethodsView;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by vaserber on 8/17/17.
 */

public class ReviewPaymentMethodsActivity extends MercadoPagoBaseActivity implements ReviewPaymentMethodsView {

    //Controls
    protected ReviewPaymentMethodsPresenter mPresenter;
    //Parameters
    protected List<PaymentMethod> mSupportedPaymentMethods;
    protected DecorationPreference mDecorationPreference;
    protected String mPublicKey;
    //View controls
    protected RecyclerView mPaymentMethodsView;
    protected ReviewPaymentMethodsAdapter mAdapter;
    protected FrameLayout mTryOtherCardButton;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        createPresenter();
        getActivityParameters();
        configurePresenter();

        if (isCustomColorSet()) {
            setTheme(R.style.Theme_MercadoPagoTheme_NoActionBar);
        }

        setContentView();
        initializeControls();
        initializeAdapter();
        setListeners();
    }

    protected void createPresenter() {
        mPresenter = new ReviewPaymentMethodsPresenter();
    }

    protected void getActivityParameters() {
        mDecorationPreference = JsonUtil.getInstance().fromJson(getIntent().getStringExtra("decorationPreference"), DecorationPreference.class);
        mPublicKey = getIntent().getStringExtra("publicKey");
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<PaymentMethod>>() {
            }.getType();
            mSupportedPaymentMethods = (gson.fromJson(this.getIntent().getStringExtra("paymentMethods"), listType));
        } catch (Exception ex) {
            ErrorUtil.startErrorActivity(this, mPresenter.getResourcesProvider().getEmptyPaymentMethodsListError(), mPublicKey);
        }
    }

    private void configurePresenter() {
        mPresenter.attachView(this);
        mPresenter.attachResourcesProvider(new ReviewPaymentMethodsProviderImpl(this));
    }

    private boolean isCustomColorSet() {
        return mDecorationPreference != null && mDecorationPreference.hasColors();
    }

    protected void setContentView() {
        setContentView(R.layout.mpsdk_activity_review_payment_methods);
    }

    protected void initializeControls() {
        mPaymentMethodsView = (RecyclerView) findViewById(R.id.mpsdkReviewPaymentMethodsView);
        mTryOtherCardButton = (FrameLayout) findViewById(R.id.tryOtherCardButton);
    }

    protected void initializeAdapter() {
        mAdapter = new ReviewPaymentMethodsAdapter(this, mSupportedPaymentMethods);
        mPaymentMethodsView.setAdapter(mAdapter);
        mPaymentMethodsView.setLayoutManager(new LinearLayoutManager(this));
    }

    protected void setListeners() {
        mTryOtherCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
