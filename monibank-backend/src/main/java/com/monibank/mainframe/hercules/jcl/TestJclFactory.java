package com.monibank.mainframe.hercules.jcl;

import org.springframework.stereotype.Component;

@Component
public class TestJclFactory {

    public String create(String jobName) {
        return """
                //%s JOB (TEST),'MONIBANK TEST',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1)
                //STEP1    EXEC PGM=IEFBR14
                //STEP2    EXEC PGM=IEBGENER
                //SYSPRINT DD SYSOUT=*
                //SYSUT1   DD *
                MONIBANK|OPERATION=CONNECTION_TEST
                MONIBANK|ACCOUNT=MB000001
                MONIBANK|AMOUNT=500.00
                MONIBANK|RESULT=SUCCESS
                /*
                //SYSUT2   DD SYSOUT=*
                //SYSIN    DD DUMMY
                //
                """.formatted(jobName);
    }
}