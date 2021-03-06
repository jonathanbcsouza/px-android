package com.mercadopago;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.mercadopago.adapters.PaymentMethodSearchItemAdapter;
import com.mercadopago.callbacks.OnSelectedCallback;
import com.mercadopago.constants.PaymentTypes;
import com.mercadopago.controllers.CheckoutTimer;
import com.mercadopago.core.MercadoPagoCheckout;
import com.mercadopago.core.MercadoPagoComponents;
import com.mercadopago.core.MercadoPagoUI;
import com.mercadopago.customviews.MPTextView;
import com.mercadopago.decorations.GridSpacingItemDecoration;
import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.Card;
import com.mercadopago.model.CustomSearchItem;
import com.mercadopago.model.Discount;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.PaymentMethodSearch;
import com.mercadopago.model.PaymentMethodSearchItem;
import com.mercadopago.model.Site;
import com.mercadopago.model.Token;
import com.mercadopago.observers.TimerObserver;
import com.mercadopago.preferences.DecorationPreference;
import com.mercadopago.preferences.FlowPreference;
import com.mercadopago.preferences.PaymentPreference;
import com.mercadopago.preferences.ServicePreference;
import com.mercadopago.presenters.PaymentVaultPresenter;
import com.mercadopago.providers.MPTrackingProvider;
import com.mercadopago.providers.PaymentVaultProviderImpl;
import com.mercadopago.px_tracking.MPTracker;
import com.mercadopago.px_tracking.model.ScreenViewEvent;
import com.mercadopago.uicontrollers.FontCache;
import com.mercadopago.uicontrollers.discounts.DiscountRowView;
import com.mercadopago.uicontrollers.paymentmethodsearch.PaymentMethodSearchCustomOption;
import com.mercadopago.uicontrollers.paymentmethodsearch.PaymentMethodSearchOption;
import com.mercadopago.uicontrollers.paymentmethodsearch.PaymentMethodSearchViewController;
import com.mercadopago.util.ApiUtil;
import com.mercadopago.util.ErrorUtil;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.util.ScaleUtil;
import com.mercadopago.util.TrackingUtil;
import com.mercadopago.views.PaymentTypesActivityView;
import com.mercadopago.views.PaymentVaultView;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentVaultActivity extends MercadoPagoBaseActivity implements PaymentVaultView, TimerObserver {

    private static final String PUBLIC_KEY_BUNDLE = "mPublicKey";
    private static final String MERCHANT_BASE_URL_BUNDLE = "mMerchantBaseUrl";
    private static final String MERCHANT_GET_CUSTOMER_URI_BUNDLE = "mMerchantGetCustomerUri";
    private static final String MERCHANT_GET_CUSTOMER_ADDITIONAL_INFO = "mMerchantGetCustomerAdditionalInfo";
    private static final String SHOW_BANK_DEALS_BUNDLE = "mShowBankDeals";
    private static final String PRESENTER_BUNDLE = "mPresenter";

    public static final String PAYMENT_VAULT_SCREEN_NAME = "PAYMENT_VAULT";
    public static final int COLUMN_SPACING_DP_VALUE = 20;
    public static final int COLUMNS = 2;

    // Local vars
    protected DecorationPreference mDecorationPreference;
    protected boolean mActivityActive;
    protected PaymentMethod mSelectedPaymentMethod;
    protected Token mToken;
    protected Issuer mSelectedIssuer;
    protected PayerCost mSelectedPayerCost;
    protected Card mSelectedCard;
    protected Context mContext;

    protected Boolean mInstallmentsEnabled;

    // Controls
    protected RecyclerView mSearchItemsRecyclerView;
    protected AppBarLayout mAppBar;

    protected PaymentVaultPresenter mPaymentVaultPresenter;
    protected CollapsingToolbarLayout mAppBarLayout;
    protected MPTextView mTimerTextView;
    protected Boolean mShowBankDeals;
    protected Boolean mEscEnabled;
    protected FrameLayout mDiscountFrameLayout;

    protected View mContentLayout;
    protected View mProgressLayout;

    protected String mPublicKey;
    protected String mPrivateKey;
    protected ServicePreference mServicePreference;

    protected String mMerchantBaseUrl;
    protected String mGetMerchantDiscountBaseURL;
    protected String mGetMerchantDiscountURI;
    protected String mMerchantGetCustomerUri;
    protected Map<String, String> mMerchantGetCustomerAdditionalInfo;
    protected Map<String, String> mGetDiscountAdditionalInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPresenter();
        getActivityParameters();

        setMerchantInfo();
        configurePresenter();

        if (isCustomColorSet()) {
            setTheme(R.style.Theme_MercadoPagoTheme_NoActionBar);
        }

        setContentView();
        initializeControls();

        //Avoid automatic selection if activity restored on back pressed from next step
        boolean selectAutomatically = savedInstanceState == null;
        initialize(selectAutomatically);
    }

    private void configurePresenter() {
        mPaymentVaultPresenter.attachView(this);
        mPaymentVaultPresenter.attachResourcesProvider(new PaymentVaultProviderImpl(this, mPublicKey, mPrivateKey, mMerchantBaseUrl, mMerchantGetCustomerUri,
                mMerchantGetCustomerAdditionalInfo, mGetMerchantDiscountBaseURL, mGetMerchantDiscountURI, mGetDiscountAdditionalInfo));
    }

    protected void setMerchantInfo() {
        if (mServicePreference != null) {
            mMerchantBaseUrl = mServicePreference.getDefaultBaseURL();
            mMerchantGetCustomerUri = mServicePreference.getGetCustomerURI();
            mMerchantGetCustomerAdditionalInfo = mServicePreference.getGetCustomerAdditionalInfo();

            mGetMerchantDiscountBaseURL = mServicePreference.getGetMerchantDiscountBaseURL();
            mGetMerchantDiscountURI = mServicePreference.getGetMerchantDiscountURI();
            mGetDiscountAdditionalInfo = mServicePreference.getGetDiscountAdditionalInfo();
        }
    }

    protected void setContentView() {
        setContentView(R.layout.mpsdk_activity_payment_vault);
    }

    protected void createPresenter() {
        mPaymentVaultPresenter = new PaymentVaultPresenter();
    }

    protected void getActivityParameters() {
        mDecorationPreference = JsonUtil.getInstance().fromJson(getIntent().getStringExtra("decorationPreference"), DecorationPreference.class);
        mServicePreference = JsonUtil.getInstance().fromJson(getIntent().getStringExtra("servicePreference"), ServicePreference.class);
        mPublicKey = getIntent().getStringExtra("merchantPublicKey");
        mPrivateKey = this.getIntent().getStringExtra("payerAccessToken");

        mPaymentVaultPresenter.setPayerAccessToken(mPrivateKey);
        mPaymentVaultPresenter.setPayerEmail(this.getIntent().getStringExtra("payerEmail"));
        mPaymentVaultPresenter.setDiscount(JsonUtil.getInstance().fromJson(getIntent().getStringExtra("discount"), Discount.class));
        mPaymentVaultPresenter.setDiscountEnabled(this.getIntent().getBooleanExtra("discountEnabled", true));
        mPaymentVaultPresenter.setDirectDiscountEnabled(this.getIntent().getBooleanExtra("directDiscountEnabled", true));
        mPaymentVaultPresenter.setInstallmentsReviewEnabled(this.getIntent().getBooleanExtra("installmentsReviewEnabled", true));
        mPaymentVaultPresenter.setMaxSavedCards(this.getIntent().getIntExtra("maxSavedCards", FlowPreference.DEFAULT_MAX_SAVED_CARDS_TO_SHOW));
        mPaymentVaultPresenter.setShowAllSavedCardsEnabled(this.getIntent().getBooleanExtra("showAllSavedCardsEnabled", false));
        mShowBankDeals = getIntent().getBooleanExtra("showBankDeals", true);
        mEscEnabled = getIntent().getBooleanExtra("escEnabled", false);

        if (getIntent().getStringExtra("paymentPreference") != null) {
            mPaymentVaultPresenter.setPaymentPreference(JsonUtil.getInstance().fromJson(getIntent().getStringExtra("paymentPreference"), PaymentPreference.class));
        }

        if (this.getIntent().getStringExtra("selectedSearchItem") != null) {
            mPaymentVaultPresenter.setSelectedSearchItem(JsonUtil.getInstance().fromJson(this.getIntent().getStringExtra("selectedSearchItem"), PaymentMethodSearchItem.class));
        }

        try {
            mPaymentVaultPresenter.setAmount(new BigDecimal(this.getIntent().getStringExtra("amount")));
        } catch (Exception ex) {
            mPaymentVaultPresenter.setAmount(null);
        }

        mPaymentVaultPresenter.setSite(JsonUtil.getInstance().fromJson(this.getIntent().getStringExtra("site"), Site.class));

        if (this.getIntent().getStringExtra("paymentMethodSearch") != null) {
            PaymentMethodSearch paymentMethodSearch = JsonUtil.getInstance().fromJson(this.getIntent().getStringExtra("paymentMethodSearch"), PaymentMethodSearch.class);
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Card>>() {
                }.getType();
                List<Card> cards = (gson.fromJson(this.getIntent().getStringExtra("cards"), listType));

                paymentMethodSearch.setCards(cards, getString(R.string.mpsdk_last_digits_label));
            } catch (Exception ex) {
                //Do nothing...
            }
            mPaymentVaultPresenter.setPaymentMethodSearch(paymentMethodSearch);
        }

        mInstallmentsEnabled = this.getIntent().getBooleanExtra("installmentsEnabled", true);
    }

    protected void initializeControls() {
        mTimerTextView = (MPTextView) findViewById(R.id.mpsdkTimerTextView);

        mDiscountFrameLayout = (FrameLayout) findViewById(R.id.mpsdkDiscount);
        mContentLayout = findViewById(R.id.mpsdkContentLayout);
        mProgressLayout = findViewById(R.id.mpsdkProgressLayout);

        initializePaymentOptionsRecyclerView();
        mAppBar = (AppBarLayout) findViewById(R.id.mpsdkAppBar);
        mAppBarLayout = (CollapsingToolbarLayout) this.findViewById(R.id.mpsdkCollapsingToolbar);
        initializeToolbar();
    }

    protected void initialize(boolean selectAutomatically) {
        showTimer();
        mPaymentVaultPresenter.initialize(selectAutomatically);
    }

    @Override
    public void trackInitialScreen() {
        MPTrackingProvider mpTrackingProvider = new MPTrackingProvider.Builder()
                .setContext(this)
                .setCheckoutVersion(BuildConfig.VERSION_NAME)
                .setPublicKey(mPublicKey)
                .build();
        ScreenViewEvent event = new ScreenViewEvent.Builder()
                .setScreenId(TrackingUtil.SCREEN_ID_PAYMENT_VAULT)
                .setScreenName(TrackingUtil.SCREEN_NAME_PAYMENT_VAULT)
                .build();
        mpTrackingProvider.addTrackEvent(event);
    }

    @Override
    public void trackChildrenScreen() {
        PaymentMethodSearchItem selectedItem = mPaymentVaultPresenter.getSelectedSearchItem();
        if (selectedItem != null) {
            String selectedItemId = selectedItem.getId();

            MPTrackingProvider mpTrackingProvider = new MPTrackingProvider.Builder()
                    .setContext(this)
                    .setCheckoutVersion(BuildConfig.VERSION_NAME)
                    .setPublicKey(mPublicKey)
                    .build();

            ScreenViewEvent event = null;

            if (selectedItemId != null) {
                event = new ScreenViewEvent.Builder()
                        .setScreenId(TrackingUtil.SCREEN_ID_PAYMENT_VAULT + "/" + selectedItemId)
                        .setScreenName(TrackingUtil.SCREEN_NAME_PAYMENT_VAULT)
                        .build();
            }
            mpTrackingProvider.addTrackEvent(event);
        }
    }

    private void showTimer() {
        if (CheckoutTimer.getInstance().isTimerEnabled()) {
            CheckoutTimer.getInstance().addObserver(this);
            mTimerTextView.setVisibility(View.VISIBLE);
            mTimerTextView.setText(CheckoutTimer.getInstance().getCurrentTime());
        }
    }

    private void initializeToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.mpsdkToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        if (isCustomColorSet()) {
            decorate(toolbar);
            mAppBarLayout.setBackgroundColor(mDecorationPreference.getBaseColor());
            mAppBarLayout.setContentScrimColor(mDecorationPreference.getBaseColor());
            if (mDecorationPreference.isDarkFontEnabled()) {
                mAppBarLayout.setExpandedTitleColor(mDecorationPreference.getDarkFontColor(this));
                mAppBarLayout.setCollapsedTitleTextColor(mDecorationPreference.getDarkFontColor(this));
                if (mTimerTextView != null) {
                    mTimerTextView.setTextColor(mDecorationPreference.getDarkFontColor(this));
                }
            }
        }
        if (FontCache.hasTypeface(FontCache.CUSTOM_REGULAR_FONT)) {
            mAppBarLayout.setCollapsedTitleTypeface(FontCache.getTypeface(FontCache.CUSTOM_REGULAR_FONT));
            mAppBarLayout.setExpandedTitleTypeface(FontCache.getTypeface(FontCache.CUSTOM_REGULAR_FONT));
        }
    }

    private boolean isCustomColorSet() {
        return mDecorationPreference != null && mDecorationPreference.hasColors();
    }

    private void decorate(Toolbar toolbar) {
        if (toolbar != null) {
            if (mDecorationPreference.hasColors()) {
                toolbar.setBackgroundColor(mDecorationPreference.getBaseColor());
            }

            decorateUpArrow(toolbar);
        }
    }

    protected void decorateUpArrow(Toolbar toolbar) {
        if (mDecorationPreference.isDarkFontEnabled()) {
            int darkFont = mDecorationPreference.getDarkFontColor(this);
            Drawable upArrow = toolbar.getNavigationIcon();
            if (upArrow != null && getSupportActionBar() != null) {
                upArrow.setColorFilter(darkFont, PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }
    }

    protected void initializePaymentOptionsRecyclerView() {
        int columns = COLUMNS;
        mSearchItemsRecyclerView = (RecyclerView) findViewById(R.id.mpsdkGroupsList);
        mSearchItemsRecyclerView.setLayoutManager(new GridLayoutManager(this, columns));
        mSearchItemsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, ScaleUtil.getPxFromDp(COLUMN_SPACING_DP_VALUE, this), true));
        PaymentMethodSearchItemAdapter groupsAdapter = new PaymentMethodSearchItemAdapter();
        mSearchItemsRecyclerView.setAdapter(groupsAdapter);
    }

    protected void populateSearchList(List<PaymentMethodSearchItem> items, OnSelectedCallback<PaymentMethodSearchItem> onSelectedCallback) {
        PaymentMethodSearchItemAdapter adapter = (PaymentMethodSearchItemAdapter) mSearchItemsRecyclerView.getAdapter();
        List<PaymentMethodSearchViewController> customViewControllers = createSearchItemsViewControllers(items, onSelectedCallback);
        adapter.addItems(customViewControllers);
        adapter.notifyItemInserted();
    }

    private void populateCustomOptionsList(List<CustomSearchItem> customSearchItems, OnSelectedCallback<CustomSearchItem> onSelectedCallback) {
        PaymentMethodSearchItemAdapter adapter = (PaymentMethodSearchItemAdapter) mSearchItemsRecyclerView.getAdapter();
        List<PaymentMethodSearchViewController> customViewControllers = createCustomSearchItemsViewControllers(customSearchItems, onSelectedCallback);
        adapter.addItems(customViewControllers);
        adapter.notifyItemInserted();
    }

    private List<PaymentMethodSearchViewController> createSearchItemsViewControllers(List<PaymentMethodSearchItem> items, final OnSelectedCallback<PaymentMethodSearchItem> onSelectedCallback) {
        List<PaymentMethodSearchViewController> customViewControllers = new ArrayList<>();
        for (final PaymentMethodSearchItem item : items) {
            PaymentMethodSearchViewController viewController = new PaymentMethodSearchOption(this, item, mDecorationPreference);
            viewController.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelectedCallback.onSelected(item);
                }
            });
            customViewControllers.add(viewController);
        }
        return customViewControllers;
    }

    private List<PaymentMethodSearchViewController> createCustomSearchItemsViewControllers(List<CustomSearchItem> customSearchItems, final OnSelectedCallback<CustomSearchItem> onSelectedCallback) {
        List<PaymentMethodSearchViewController> customViewControllers = new ArrayList<>();
        for (final CustomSearchItem item : customSearchItems) {
            PaymentMethodSearchCustomOption viewController = new PaymentMethodSearchCustomOption(this, item);
            viewController.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelectedCallback.onSelected(item);
                }
            });
            customViewControllers.add(viewController);
        }
        return customViewControllers;
    }

    @Override
    public void startSavedCardFlow(Card card, BigDecimal amount) {
        new MercadoPagoComponents.Activities.CardVaultActivityBuilder()
                .setActivity(this)
                .setMerchantPublicKey(mPublicKey)
                .setPayerAccessToken(mPrivateKey)
                .setAmount(amount)
                .setSite(mPaymentVaultPresenter.getSite())
                .setCard(card)
                .setDecorationPreference(mDecorationPreference)
                .setPaymentPreference(mPaymentVaultPresenter.getPaymentPreference())
                .setInstallmentsEnabled(mInstallmentsEnabled)
                .setPayerEmail(mPaymentVaultPresenter.getPayerEmail())
                .setDiscount(mPaymentVaultPresenter.getDiscount())
                .setDiscountEnabled(mPaymentVaultPresenter.getDiscountEnabled())
                .setDirectDiscountEnabled(mPaymentVaultPresenter.getDirectDiscountEnabled())
                .setInstallmentsReviewEnabled(mPaymentVaultPresenter.getInstallmentsReviewEnabled())
                .setShowBankDeals(mShowBankDeals)
                .setESCEnabled(mEscEnabled)
                .startActivity();
        animatePaymentMethodSelection();
    }

    @Override
    public void showSelectedItem(PaymentMethodSearchItem item) {
        Intent intent = new Intent(this, PaymentVaultActivity.class);
        intent.putExtras(this.getIntent());
        intent.putExtra("selectedSearchItem", JsonUtil.getInstance().toJson(item));
        intent.putExtra("discount", JsonUtil.getInstance().toJson(mPaymentVaultPresenter.getDiscount()));
        intent.putExtra("paymentMethodSearch", JsonUtil.getInstance().toJson(mPaymentVaultPresenter.getPaymentMethodSearch()));
        intent.putExtra("discountEnabled", mPaymentVaultPresenter.getDiscountEnabled());
        intent.putExtra("directDiscountEnabled", mPaymentVaultPresenter.getDirectDiscountEnabled());

        startActivityForResult(intent, MercadoPagoComponents.Activities.PAYMENT_VAULT_REQUEST_CODE);
        overridePendingTransition(R.anim.mpsdk_slide_right_to_left_in, R.anim.mpsdk_slide_right_to_left_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MercadoPagoComponents.Activities.CARD_VAULT_REQUEST_CODE) {
            resolveCardRequest(resultCode, data);
        } else if (requestCode == MercadoPagoComponents.Activities.PAYMENT_METHODS_REQUEST_CODE) {
            resolvePaymentMethodsRequest(resultCode, data);
        } else if (requestCode == MercadoPagoComponents.Activities.PAYMENT_VAULT_REQUEST_CODE) {
            resolvePaymentVaultRequest(resultCode, data);
        } else if (requestCode == MercadoPagoComponents.Activities.DISCOUNTS_REQUEST_CODE) {
            resolveDiscountRequest(resultCode, data);
        } else if (requestCode == ErrorUtil.ERROR_REQUEST_CODE) {
            resolveErrorRequest(resultCode, data);
        }
    }

    private void resolveErrorRequest(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            recoverFromFailure();
        } else if (mPaymentVaultPresenter.isItemSelected()) {
            hideProgress();
        } else {
            setResult(resultCode, data);
            finish();
        }
    }

    private void recoverFromFailure() {
        mPaymentVaultPresenter.recoverFromFailure();
    }

    private void resolvePaymentVaultRequest(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        } else if (resultCode == RESULT_CANCELED && data != null && data.hasExtra("mercadoPagoError")) {
            setResult(Activity.RESULT_CANCELED, data);
            this.finish();
        } else {
            //When it comes back from payment vault "children" view
            trackInitialScreen();

            if (shouldFinishOnBack(data)) {
                setResult(Activity.RESULT_CANCELED, data);
                this.finish();
            } else {
                Discount discount = JsonUtil.getInstance().fromJson(data.getStringExtra("discount"), Discount.class);
                if (discount != null) {
                    mPaymentVaultPresenter.setDiscount(discount);
                    mPaymentVaultPresenter.initializeDiscountRow();
                }
            }
        }
    }

    protected void resolveCardRequest(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mSelectedPaymentMethod = JsonUtil.getInstance().fromJson(data.getStringExtra("paymentMethod"), PaymentMethod.class);
            mToken = JsonUtil.getInstance().fromJson(data.getStringExtra("token"), Token.class);
            mSelectedIssuer = JsonUtil.getInstance().fromJson(data.getStringExtra("issuer"), Issuer.class);
            mSelectedPayerCost = JsonUtil.getInstance().fromJson(data.getStringExtra("payerCost"), PayerCost.class);
            mSelectedCard = JsonUtil.getInstance().fromJson(data.getStringExtra("card"), Card.class);

            Discount discount = JsonUtil.getInstance().fromJson(data.getStringExtra("discount"), Discount.class);
            if (discount != null) {
                mPaymentVaultPresenter.setDiscount(discount);
                mPaymentVaultPresenter.initializeDiscountRow();
            }

            finishWithCardResult();
        } else {

            trackChildrenScreen();

            if (shouldFinishOnBack(data)) {
                setResult(Activity.RESULT_CANCELED, data);
                this.finish();
            } else {
                overridePendingTransition(R.anim.mpsdk_slide_left_to_right_in, R.anim.mpsdk_slide_left_to_right_out);
            }
            Discount discount;
            if (data != null && data.getStringExtra("discount") != null) {
                discount = JsonUtil.getInstance().fromJson(data.getStringExtra("discount"), Discount.class);
                mPaymentVaultPresenter.setDiscount(discount);
                mPaymentVaultPresenter.initializeDiscountRow();
            }
        }
    }

    protected void resolvePaymentMethodsRequest(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            PaymentMethod paymentMethod = JsonUtil.getInstance().fromJson(data.getStringExtra("paymentMethod"), PaymentMethod.class);
            selectPaymentMethod(paymentMethod);
        }
    }

    protected void resolveDiscountRequest(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (mPaymentVaultPresenter.getDiscount() == null) {
                Discount discount = JsonUtil.getInstance().fromJson(data.getStringExtra("discount"), Discount.class);
                mPaymentVaultPresenter.onDiscountReceived(discount);
            }
        }
    }

    private boolean shouldFinishOnBack(Intent data) {
        return mPaymentVaultPresenter.getSelectedSearchItem() != null && (!mPaymentVaultPresenter.getSelectedSearchItem().hasChildren() || mPaymentVaultPresenter.getSelectedSearchItem().getChildren().size() == 1)
                || (mPaymentVaultPresenter.getSelectedSearchItem() == null && (mPaymentVaultPresenter.isOnlyUniqueSearchSelectionAvailable() || mPaymentVaultPresenter.isOnlyAccountMoneyEnabled()))
                || (data != null) && (data.getStringExtra("mercadoPagoError") != null);
    }

    @Override
    public void cleanPaymentMethodOptions() {
        PaymentMethodSearchItemAdapter adapter = (PaymentMethodSearchItemAdapter) mSearchItemsRecyclerView.getAdapter();
        adapter.clear();
    }

    @Override
    public void selectPaymentMethod(PaymentMethod paymentMethod) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("paymentMethod", JsonUtil.getInstance().toJson(paymentMethod));
        returnIntent.putExtra("discount", JsonUtil.getInstance().toJson(mPaymentVaultPresenter.getDiscount()));

        this.setResult(Activity.RESULT_OK, returnIntent);
        this.finish();
        animatePaymentMethodSelection();
    }

    protected void finishWithCardResult() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("token", JsonUtil.getInstance().toJson(mToken));
        if (mSelectedIssuer != null) {
            returnIntent.putExtra("issuer", JsonUtil.getInstance().toJson(mSelectedIssuer));
        }
        returnIntent.putExtra("payerCost", JsonUtil.getInstance().toJson(mSelectedPayerCost));
        returnIntent.putExtra("paymentMethod", JsonUtil.getInstance().toJson(mSelectedPaymentMethod));
        returnIntent.putExtra("discount", JsonUtil.getInstance().toJson(mPaymentVaultPresenter.getDiscount()));
        returnIntent.putExtra("card", JsonUtil.getInstance().toJson(mSelectedCard));

        this.setResult(Activity.RESULT_OK, returnIntent);
        this.finish();
        animatePaymentMethodSelection();
    }

    private void animatePaymentMethodSelection() {
        overridePendingTransition(R.anim.mpsdk_slide_right_to_left_in, R.anim.mpsdk_slide_right_to_left_out);
    }

    @Override
    public void showProgress() {
        mProgressLayout.setVisibility(View.VISIBLE);
        mAppBar.setVisibility(View.GONE);
        mContentLayout.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress() {
        mProgressLayout.setVisibility(View.GONE);
        mAppBar.setVisibility(View.VISIBLE);
        mContentLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void setTitle(String title) {
        if (mAppBarLayout != null) {
            mAppBarLayout.setTitle(title);
        }
    }

    @Override
    public void startCardFlow(String paymentType, BigDecimal amount, Boolean automaticSelection) {
        PaymentPreference paymentPreference = mPaymentVaultPresenter.getPaymentPreference();
        paymentPreference.setDefaultPaymentTypeId(paymentType);

        new MercadoPagoComponents.Activities.CardVaultActivityBuilder()
                .setActivity(this)
                .setMerchantPublicKey(mPublicKey)
                .setPayerAccessToken(mPrivateKey)
                .setPaymentPreference(paymentPreference)
                .setDecorationPreference(mDecorationPreference)
                .setAmount(amount)
                .setSite(mPaymentVaultPresenter.getSite())
                .setInstallmentsEnabled(mInstallmentsEnabled)
                .setPayerEmail(mPaymentVaultPresenter.getPayerEmail())
                .setDiscount(mPaymentVaultPresenter.getDiscount())
                .setAutomaticSelection(automaticSelection)
                .setDiscountEnabled(mPaymentVaultPresenter.getDiscountEnabled())
                .setDirectDiscountEnabled(mPaymentVaultPresenter.getDirectDiscountEnabled())
                .setInstallmentsReviewEnabled(mPaymentVaultPresenter.getInstallmentsReviewEnabled())
                .setShowBankDeals(mShowBankDeals)
                .setESCEnabled(mEscEnabled)
                .setAcceptedPaymentMethods(mPaymentVaultPresenter.getPaymentMethodSearch().getPaymentMethods())
                .startActivity();
        animatePaymentMethodSelection();
    }

    @Override
    public void startPaymentMethodsSelection() {
        new MercadoPagoComponents.Activities.PaymentMethodsActivityBuilder()
                .setActivity(this)
                .setMerchantPublicKey(mPublicKey)
                .setPaymentPreference(mPaymentVaultPresenter.getPaymentPreference())
                .setDecorationPreference(mDecorationPreference)
                .startActivity();
    }

    public void showApiException(ApiException apiException, String requestOrigin) {
        if (mActivityActive) {
            ApiUtil.showApiExceptionError(this, apiException, mPublicKey, requestOrigin);
        }
    }

    @Override
    public void showCustomOptions(List<CustomSearchItem> customSearchItems, OnSelectedCallback<CustomSearchItem> customSearchItemOnSelectedCallback) {
        populateCustomOptionsList(customSearchItems, customSearchItemOnSelectedCallback);
    }

    @Override
    public void showSearchItems(List<PaymentMethodSearchItem> searchItems, OnSelectedCallback<PaymentMethodSearchItem> paymentMethodSearchItemSelectionCallback) {
        populateSearchList(searchItems, paymentMethodSearchItemSelectionCallback);
    }

    @Override
    public void showError(MercadoPagoError error, String requestOrigin) {
        if (error.isApiException()) {
            showApiException(error.getApiException(), requestOrigin);
        } else {
            ErrorUtil.startErrorActivity(this, error, mPublicKey);
        }
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("discount", JsonUtil.getInstance().toJson(mPaymentVaultPresenter.getDiscount()));
        setResult(RESULT_CANCELED, returnIntent);
        finish();

        if (mPaymentVaultPresenter.isItemSelected()) {
            overridePendingTransition(R.anim.mpsdk_slide_left_to_right_in, R.anim.mpsdk_slide_left_to_right_out);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PUBLIC_KEY_BUNDLE, mPublicKey);
        outState.putString(MERCHANT_GET_CUSTOMER_URI_BUNDLE, mMerchantGetCustomerUri);
        outState.putString(MERCHANT_BASE_URL_BUNDLE, mMerchantBaseUrl);
        outState.putString(MERCHANT_GET_CUSTOMER_ADDITIONAL_INFO, JsonUtil.getInstance().toJson(mMerchantGetCustomerAdditionalInfo));
        outState.putBoolean(SHOW_BANK_DEALS_BUNDLE, mShowBankDeals);
        outState.putString(PRESENTER_BUNDLE, JsonUtil.getInstance().toJson(mPaymentVaultPresenter));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        mPublicKey = savedInstanceState.getString(PUBLIC_KEY_BUNDLE);
        String merchantGetCustomerAdditionalInfo = savedInstanceState.getString(MERCHANT_GET_CUSTOMER_ADDITIONAL_INFO);
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        mMerchantGetCustomerAdditionalInfo = new Gson().fromJson(merchantGetCustomerAdditionalInfo, type);
        mMerchantGetCustomerUri = savedInstanceState.getString(MERCHANT_GET_CUSTOMER_URI_BUNDLE);
        mMerchantBaseUrl = savedInstanceState.getString(MERCHANT_BASE_URL_BUNDLE);
        mShowBankDeals = savedInstanceState.getBoolean(SHOW_BANK_DEALS_BUNDLE, true);
        mPaymentVaultPresenter = JsonUtil.getInstance().fromJson(savedInstanceState.getString(PRESENTER_BUNDLE), PaymentVaultPresenter.class);
        configurePresenter();

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        mActivityActive = true;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mActivityActive = false;
        mPaymentVaultPresenter.detachView();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mActivityActive = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        mActivityActive = false;
        super.onStop();
    }

    @Override
    public void onTimeChanged(String timeToShow) {
        mTimerTextView.setText(timeToShow);
    }

    @Override
    public void onFinish() {
        setResult(MercadoPagoCheckout.TIMER_FINISHED_RESULT_CODE);
        this.finish();
    }

    @Override
    public void startDiscountFlow(BigDecimal transactionAmount) {
        MercadoPagoComponents.Activities.DiscountsActivityBuilder mercadoPagoBuilder
                = new MercadoPagoComponents.Activities.DiscountsActivityBuilder();

        mercadoPagoBuilder.setActivity(this)
                .setMerchantPublicKey(mPublicKey)
                .setPayerEmail(mPaymentVaultPresenter.getPayerEmail())
                .setAmount(transactionAmount)
                .setDiscount(mPaymentVaultPresenter.getDiscount())
                .setDirectDiscountEnabled(mPaymentVaultPresenter.getDirectDiscountEnabled())
                .setDecorationPreference(mDecorationPreference);

        if (mPaymentVaultPresenter.getDiscount() == null) {
            mercadoPagoBuilder.setDirectDiscountEnabled(false);
        } else {
            mercadoPagoBuilder.setDiscount(mPaymentVaultPresenter.getDiscount());
        }

        mercadoPagoBuilder.startActivity();
    }

    @Override
    public void showDiscount(BigDecimal transactionAmount) {
        DiscountRowView discountRowView = new MercadoPagoUI.Views.DiscountRowViewBuilder()
                .setContext(this)
                .setDiscount(mPaymentVaultPresenter.getDiscount())
                .setTransactionAmount(transactionAmount)
                .setCurrencyId(mPaymentVaultPresenter.getSite().getCurrencyId())
                .setDiscountEnabled(mPaymentVaultPresenter.getDiscountEnabled())
                .build();

        discountRowView.inflateInParent(mDiscountFrameLayout, true);
        discountRowView.initializeControls();
        discountRowView.draw();
        discountRowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPaymentVaultPresenter.getDiscountEnabled()) {
                    mPaymentVaultPresenter.onDiscountOptionSelected();
                }
            }
        });
    }
}
