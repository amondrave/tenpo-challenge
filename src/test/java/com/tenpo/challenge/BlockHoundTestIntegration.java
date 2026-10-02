package com.tenpo.challenge;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

public class BlockHoundTestIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside(
            "org.hibernate.validator.resourceloading.PlatformResourceBundleLocator",
            "getResourceBundle");
        builder.allowBlockingCallsInside(
            "com.ongres.scram.common.util.CryptoUtil",
            "nonce");
    }
}
