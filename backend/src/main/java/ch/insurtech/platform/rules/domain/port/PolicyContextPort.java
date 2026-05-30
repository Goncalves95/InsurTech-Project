package ch.insurtech.platform.rules.domain.port;

import ch.insurtech.platform.rules.domain.model.PolicyContext;

public interface PolicyContextPort {

    PolicyContext loadForPolicyHolder(String policyHolderId);
}
