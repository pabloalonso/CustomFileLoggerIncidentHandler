package com.bonitasoft.customfileloggerincidenthandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.incident.Incident;
import org.bonitasoft.engine.incident.IncidentHandler;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

/**
 *
 * @author Domenico, Pablo Alonso
 */
public class FileLoggerIncidentHandlerCustom implements IncidentHandler {

    private final Map<Long, Logger> loggers;
    private TechnicalLoggerService technicalLoggerService;
    private Long timeToWait;
    private String adminUser;
    private String adminPwd;

    public FileLoggerIncidentHandlerCustom() {
        loggers = new HashMap<>(2);
    }

    public FileLoggerIncidentHandlerCustom(TechnicalLoggerService technicalLoggerService, String adminUser, String adminPwd, Long timeToWait) {
        this.timeToWait = timeToWait;
        loggers = new HashMap<>(2);
        this.adminPwd = adminPwd;
        this.adminUser = adminUser;
        this.timeToWait=timeToWait;
        this.technicalLoggerService = technicalLoggerService;
    }

    @Override
    public void handle(long tenantId, Incident incident) {
        Logger logger = null;
        try {
            logger = getLogger(tenantId);
            logger.log(Level.SEVERE, "An incident occurred: " + incident.getDescription());
            logger.log(Level.SEVERE, "Exception was", incident.getCause());
            logger.log(Level.SEVERE, "We were unable to handle the failure on the elements because of", incident.getExceptionWhenHandlingFailure());
            final String recoveryProcedure = incident.getRecoveryProcedure();
            if (recoveryProcedure != null && !recoveryProcedure.isEmpty()) {
                logger.log(Level.SEVERE, "Procedure to recover: " + recoveryProcedure);
                if (recoveryProcedure.contains("processApi.executeFlowNode")) {
                    Long activityId = null;
                    Matcher m = Pattern.compile("\\((.*?)\\)").matcher(recoveryProcedure);
                    while (m.find()) {
                        activityId = Long.valueOf(m.group(1));
                    }
                    RetryTask retryTask = new RetryTask();
                    retryTask.setTaskId(activityId);
                    retryTask.setTechnicalLog(technicalLoggerService);
                    retryTask.setAdminPwd(adminPwd);
                    retryTask.setAdminUser(adminUser);
                    retryTask.setTimeToWait(timeToWait);
                    Thread delayedRetryThread = new Thread(retryTask);
                    delayedRetryThread.start();
                }

            }
        } catch (final SecurityException | IOException | BonitaHomeNotSetException e) {
            logger.log(Level.SEVERE, "We were unable to handle the failure on the elements because of", e);
        }
    }

    protected Logger getLogger(final long tenantId) throws SecurityException, IOException, BonitaHomeNotSetException {
        Logger logger = loggers.get(tenantId);
        if (logger == null) {
            logger = Logger.getLogger("INCIDENT" + tenantId);
            final FileHandler fh = BonitaHomeServer.getInstance().getTenantStorage().getIncidentFileHandler(tenantId);
            logger.addHandler(fh);
            final SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            loggers.put(tenantId, logger);
        }
        return logger;
    }
}
