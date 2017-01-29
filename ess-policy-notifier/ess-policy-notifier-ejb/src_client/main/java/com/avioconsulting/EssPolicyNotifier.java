package com.avioconsulting;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface EssPolicyNotifier {
    List<String> createPolicyAssembly(String hostingApp, String essPackage, String jobName) throws Exception;
    List<String> deletePolicyAssembly(String hostingApp, String essPackage, String jobName) throws Exception;
}
