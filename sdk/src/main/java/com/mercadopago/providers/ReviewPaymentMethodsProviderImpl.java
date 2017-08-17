package com.mercadopago.providers;

import android.content.Context;

import com.mercadopago.R;
import com.mercadopago.exceptions.MercadoPagoError;

/**
 * Created by vaserber on 8/17/17.
 */

public class ReviewPaymentMethodsProviderImpl implements ReviewPaymentMethodsProvider {

    private final Context context;

    public ReviewPaymentMethodsProviderImpl(Context context) {
        this.context = context;
    }

    @Override
    public MercadoPagoError getEmptyPaymentMethodsListError() {
        String message = getStandardErrorMessage();
        String detail = context.getString(R.string.mpsdk_error_message_detail_no_payment_method_list);

        return new MercadoPagoError(message, detail, false);
    }

    public String getStandardErrorMessage() {
        return context.getString(R.string.mpsdk_standard_error_message);
    }
}
