/*
 * Swagger HIP Facade - OpenAPI 3.0
 * This is a set of interfaces based on the OpenAPI 3.0 specification for a wrapper client
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.nha.abdm.wrapper.client.api;

import com.nha.abdm.wrapper.client.invoker.ApiException;
import com.nha.abdm.wrapper.client.model.RequestOtpPostRequest;
import com.nha.abdm.wrapper.client.model.RequestStatusResponse;
import com.nha.abdm.wrapper.client.model.VerifyOtpPostRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for DiscoveryApi
 */
@Disabled
public class DiscoveryApiTest {

    private final DiscoveryApi api = new DiscoveryApi();

    /**
     * The Initiating of otp in discovery flow
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void requestOtpPostTest() throws ApiException {
        RequestOtpPostRequest requestOtpPostRequest = null;
        RequestStatusResponse response = api.requestOtpPost(requestOtpPostRequest);
        // TODO: test validations
    }

    /**
     * The Verification of otp in discovery flow
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void verifyOtpPostTest() throws ApiException {
        VerifyOtpPostRequest verifyOtpPostRequest = null;
        RequestStatusResponse response = api.verifyOtpPost(verifyOtpPostRequest);
        // TODO: test validations
    }

}