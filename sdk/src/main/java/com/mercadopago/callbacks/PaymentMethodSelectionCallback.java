package com.mercadopago.callbacks;

import com.mercadopago.model.PaymentMethod;

import java.util.List;

public interface PaymentMethodSelectionCallback {
    void onPaymentMethodListSet(List<PaymentMethod> paymentMethodList, String bin);
//    void onPaymentMethodSet(PaymentMethod paymentMethod);
    void onPaymentMethodCleared();
}
