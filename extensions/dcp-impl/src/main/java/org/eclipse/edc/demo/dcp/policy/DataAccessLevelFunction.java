/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

public class DataAccessLevelFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Duty, C> {
    private final Monitor monitor;

    private static final String DATAPROCESSOR_CRED_TYPE = "DataProcessorCredential";
    private static final String CONTRACT_VERSION_CLAIM = "contractVersion";
    private static final String LEVEL_CLAIM = "level";

    private DataAccessLevelFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    public static <C extends ParticipantAgentPolicyContext> DataAccessLevelFunction<C> create(Monitor monitor) {
        return new DataAccessLevelFunction<>(monitor) {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, C policyContext) {
        monitor.debug("DataAccessLevelFunction evaluate called");

        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            monitor.severe("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }

        var pa = policyContext.participantAgent();
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            monitor.severe("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            monitor.severe(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(DATAPROCESSOR_CRED_TYPE)))
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .anyMatch(credentialSubject -> {
                    var version = credentialSubject.getClaim(MVD_NAMESPACE, CONTRACT_VERSION_CLAIM);
                    var level = credentialSubject.getClaim(MVD_NAMESPACE, LEVEL_CLAIM);

                    monitor.debug("Version: %s".formatted(version));
                    monitor.debug("Level: %s".formatted(level));

                    return version != null && Objects.equals(level, rightOperand);
                });
    }
}
