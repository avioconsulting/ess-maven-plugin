/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avioconsulting;

import oracle.as.scheduler.MetadataObjectId;
import oracle.as.scheduler.core.WsmPolicyUpdateEvent;
import oracle.as.scheduler.security.internal.owsm.EssPolicyManager;
import oracle.as.scheduler.security.internal.owsm.JobPolicyAssembly;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class EssPolicyNotifierBean implements EssPolicyNotifier {
    @Override
    public List<String> createPolicyAssembly(String hostingApp, String essPackage, String jobName) throws Exception {
        List<String> logMessages = new ArrayList<>();
        EssPolicyManager manager = getEssPolicyManager(hostingApp);
        JobPolicyAssembly existingAssembly = getExistingAssembly(manager, essPackage, jobName);
        if (existingAssembly == null) {
            log(logMessages, "Creating assembly");
            manager.createJobPolicyAssembly(essPackage, jobName);
            log(logMessages, "Created assembly, firing update event!");
            MetadataObjectId jobDefinitionId = getJobDefinitionId(essPackage, jobName);
            manager.publishUpdateEvent(hostingApp,
                    WsmPolicyUpdateEvent.UpdateType.CREATE,
                    jobDefinitionId);
            log(logMessages, "Update event fired!");
        }
        return logMessages;
    }

    private MetadataObjectId getJobDefinitionId(String essPackage, String jobName) {
        return MetadataObjectId.createMetadataObjectId(MetadataObjectId.MetadataObjectType.JOB_DEFINITION,
                essPackage,
                jobName);
    }

    private void log(List<String> logMessages, String message) {
        System.out.println(message);
        logMessages.add(message);
    }

    private JobPolicyAssembly getExistingAssembly(EssPolicyManager manager, String essPackage, String jobName) {
        try {
            return manager.getJobPolicyAssembly(essPackage, jobName);
        } catch (Exception e) {
            return null;
        }
    }

    private EssPolicyManager getEssPolicyManager(String hostingApp) {
        return EssPolicyManager.getInstanceByLogicalAppName(hostingApp);
    }
}
