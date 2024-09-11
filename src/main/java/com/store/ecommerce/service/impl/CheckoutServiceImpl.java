package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.CartItemDTO;
import com.store.ecommerce.dto.CheckoutInfo;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CurrencyRepository;
import com.store.ecommerce.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    @Autowired
    private ShippingRateService shippingRateService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Override
    public CheckoutInfo getCheckoutInformation(String email) throws NotFoundException {
        // Check for existence of user email
        UserDTO userByEmail = userService.getUserByEmail(email);

        // Get default address
        Address defaultAddress = addressService.getDefaultAddress(email);

        // Get shipping rate from default address
        ShippingRate shippingRate = shippingRateService.getShippingRateByCountryAndState(
                defaultAddress.getCountry(), defaultAddress.getState());

        if (shippingRate == null) {
            throw new NotFoundException("Default address does not supported for shipping!");
        }

        CartDTO cart = cartService.findByUserEmail(email);
        List<CartItemDTO> items = cart.getItems();

        float shippingCostTotal = calculateShippingCost(items, shippingRate);

        SettingBag generalSettings = settingService.getGeneralSettingBag();
        Long currencyId = Long.valueOf(generalSettings.getValue("CURRENCY_ID"));
        Currency currency = currencyRepository.findById(currencyId).orElseThrow(
                () -> new NotFoundException("Could not find any currency with ID: " + currencyId));

        return CheckoutInfo.builder()
                .listItems(items)
                .address(defaultAddress.toString())
                .productTotal(cart.getTotal())
                .shippingCostTotal(shippingCostTotal)
                .paymentTotal(shippingCostTotal + cart.getTotal())
                .deliverDays(shippingRate.getDays())
                .deliverDate(getDeliverDate(shippingRate.getDays()))
                .codSupported(shippingRate.isCodSupported())
                .currencyCode(currency.getCode())
                .build();
    }

    private float calculateShippingCost(List<CartItemDTO> items, ShippingRate shippingRate) {
        float shippingCostTotal = 0.0f;

        for (CartItemDTO item : items) {
            float shippingCost = (float) Math.round(item.getItemWeight() * item.getQuantity() * shippingRate.getRate() * 100) /100;
            item.setShippingCost(shippingCost);
            shippingCostTotal += shippingCost;
        }

        return shippingCostTotal;
    }

    public Date getDeliverDate(int deliverDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, deliverDays);

        return calendar.getTime();
    }
}
