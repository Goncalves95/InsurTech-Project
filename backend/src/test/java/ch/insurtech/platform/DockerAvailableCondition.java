package ch.insurtech.platform;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 ExecutionCondition that disables a test class when Docker is not available.
 * Evaluated before SpringExtension loads the application context, so disabled tests
 * incur zero startup cost and produce a clean "skipped" result in Failsafe reports.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Docker is available");
    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("Docker is not available — integration test skipped");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            return DockerClientFactory.instance().isDockerAvailable() ? ENABLED : DISABLED;
        } catch (Exception e) {
            return DISABLED;
        }
    }
}
