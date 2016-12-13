/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Domenico
 */
public class TestMatcher {

    public TestMatcher() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testMatche() {
        String recoveryProcedure="Procedure to recover: call processApi.executeFlowNode(3420152)";
        Long activityId = null;
        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(recoveryProcedure);
        while (m.find()) {
            activityId = Long.valueOf(m.group(1));
        }
        System.out.println(activityId);
    }
}
