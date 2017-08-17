package com.mercadopago.providers;

import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.mvp.ResourcesProvider;

/**
 * Created by vaserber on 8/17/17.
 */

public interface ReviewPaymentMethodsProvider extends ResourcesProvider {

    MercadoPagoError getEmptyPaymentMethodsListError();
}
