package com.avioconsulting.ess.mojos

class FieldConstants {
    // we're remote and not within a module so we need the global JNDI namespace
    public static
    final String ESS_JNDI_EJB_METADATA = 'java:global.EssNativeHostingApp.native-ess-ejb.MetadataServiceBean!oracle.as.scheduler.MetadataServiceRemote'
    public static
    final String ESS_JNDI_EJB_RUNTIME = 'java:global.EssNativeHostingApp.native-ess-ejb.RuntimeServiceBean!oracle.as.scheduler.RuntimeServiceRemote'
}
