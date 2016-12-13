/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bonitasoft.customfileloggerincidenthandler;

import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessRuntimeAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.session.APISession;

/**
 *
 * @author Domenico, Pablo Alonso
 */
public class RetryTask implements Runnable {

    /**
     * Id of the task to run again.
     */
    private long taskId;
    private String adminUser;
    private String adminPwd;
    private Long timeToWait;

    /**
     * Bonita technical logger.
     */
    private TechnicalLoggerService technicalLog;

    public void setTaskId(long id) {
        taskId = id;
    }

    public void setTechnicalLog(TechnicalLoggerService technicalLog) {
        this.technicalLog = technicalLog;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public void setAdminPwd(String adminPwd) {
        this.adminPwd = adminPwd;
    }

    public void setTimeToWait(Long timeToWait) {
        this.timeToWait = timeToWait;
    }
    

    @Override
    public void run() {
        try {
            StringBuilder message = new StringBuilder();
            message.append("Starting to wait before trying again task execution ");
            message.append(taskId);
            technicalLog.log(this.getClass(), TechnicalLogSeverity.INFO, message.toString());
            Thread.sleep(timeToWait);

            StringBuilder message2 = new StringBuilder();
            message2.append("Will now try task execution ");
            message2.append(taskId);
            technicalLog.log(this.getClass(), TechnicalLogSeverity.INFO,
                    message2.toString());
            LoginAPI loginAPI = null;
            APISession session = null;
            try {
                // Get the LoginAPI using the TenantAPIAccessor
                loginAPI = TenantAPIAccessor.getLoginAPI();

                // Log in to the tenant to create a session
                session = loginAPI.login(adminUser, adminPwd);
                // Get the ProcessRuntimeAPI using the TenantAPIAccessor and the
                // previously created session
                ProcessRuntimeAPI processAPI = TenantAPIAccessor.getProcessAPI(session);

                processAPI.executeFlowNode(taskId);
            }catch(FlowNodeExecutionException fn){
                technicalLog.log(this.getClass(), TechnicalLogSeverity.INFO,
                        "Task with id: "
                                + taskId + " not found, it might be executed before", fn);
            } catch (Exception e) {
                technicalLog.log(this.getClass(), TechnicalLogSeverity.ERROR,
                        "Error while retyring task execution. Task id: "
                                + taskId, e);
            }finally{
                try {
                    loginAPI.logout(session);
                }catch (Exception ex){
                    technicalLog.log(this.getClass(), TechnicalLogSeverity.ERROR,
                            "Error while closing Session ", ex);
                }
            }

        }catch(Exception ex){
                technicalLog.log(this.getClass(), TechnicalLogSeverity.ERROR,
                        "Error while retyring task execution. Task id: "
                                + taskId, ex);
         }
        return;
    }
}
