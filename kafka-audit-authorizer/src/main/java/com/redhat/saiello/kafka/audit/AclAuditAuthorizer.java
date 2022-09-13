package com.redhat.saiello.kafka.audit;

import kafka.security.authorizer.AclAuthorizer;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.utils.SecurityUtils;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;



public class AclAuditAuthorizer extends AclAuthorizer {

    private final static String AUDIT_CONFIG_INCLUDE_SUPER_USERS = "audit.authorizer.include.super.users";
    private final static String AUDIT_CONFIG_INCLUDE_USERS = "audit.authorizer.include.users";
    private final static String AUDIT_CONFIG_EXCLUDE_USERS = "audit.authorizer.exclude.users";

    private final static Logger logger = LoggerFactory.getLogger(AclAuditAuthorizer.class);
    private final static Logger audit = LoggerFactory.getLogger("audit");

    private boolean includeSuperUser;

    private Collection<String> includeUsers;

    private Collection<String> excludeUsers;

    @Override
    public void configure(Map<String, ?> javaConfigs) {
        super.configure(javaConfigs);

        String strIncludeSuperUsers = (String)javaConfigs.get(AUDIT_CONFIG_INCLUDE_SUPER_USERS);
        String strIncludeUsers = (String)javaConfigs.get(AUDIT_CONFIG_INCLUDE_USERS);
        String strExcludeUsers = (String)javaConfigs.get(AUDIT_CONFIG_EXCLUDE_USERS);
        
        this.includeSuperUser = (strIncludeSuperUsers!=null) ? Boolean.parseBoolean(strIncludeSuperUsers) : true;
        this.includeUsers = (strIncludeUsers!=null) ? Arrays.asList(strIncludeUsers.split(";")) : Collections.emptyList();
        this.excludeUsers = (strExcludeUsers!=null) ? Arrays.asList(strExcludeUsers.split(";")) : Collections.emptyList();

        logger.info(
            "AuditConfig includeSuperUser={} includeUsers={} excludeUsers={}", includeSuperUser, includeUsers, excludeUsers);
    }


    @Override
    public List<AuthorizationResult> authorize(AuthorizableRequestContext requestContext, List<Action> actions) {
        
        KafkaPrincipal principal = requestContext.principal();
        logger.trace("Authorize principal={} requestContext={} actions={}", principal, requestContext, actions);
        
        List<AuthorizationResult> authorizationResults = super.authorize(requestContext, actions);
        
        if(needToAudit(principal)){
            int actionsSize = actions.size();
            for(int i=0; i < actionsSize; i++){
                logCustomAuditMessage(requestContext, actions.get(i), authorizationResults.get(i));
            }               
        }

        return authorizationResults;
    }

    private boolean needToAudit(KafkaPrincipal principal){
        return (includeSuperUser && isSuperUser(principal) && !excludeUsers.contains(principal.toString())) // is a super user not explicitly excluded
            || includeUsers.contains(principal.toString()); // is a explicitly included user 
    }


    private void logCustomAuditMessage(AuthorizableRequestContext requestContext, Action action, AuthorizationResult result){
        
        String resource = String.format("%s-%s-%s", 
            SecurityUtils.resourceTypeName(action.resourcePattern().resourceType()),
            action.resourcePattern().patternType(),
            action.resourcePattern().name());

        String apiKey = ApiKeys.hasId(requestContext.requestType()) ? ApiKeys.forId(requestContext.requestType()).name : Integer.toString(requestContext.requestType());

        audit.info(
            "Principal = {} is {} Operation = {} " + 
            "from host = {} on resource = {} for request = {} with resourceRefCount = {}",
            requestContext.principal(),
            result,
            SecurityUtils.operationName(action.operation()),
            requestContext.clientAddress().getHostAddress(),
            resource,
            apiKey,
            action.resourceReferenceCount()
            );
    }

}
