package com.example.pentaho.component;

import org.springframework.stereotype.Component;

public class PentahoWebService  {

    /**
     * 目前的寫法是直接呼叫PentahoServer上的檔案，非PentahoRepository
     * /home/addr/ADDR_FULL_FLOW/1_PRE_PROCESS/MAX_LOG_ID.ktr
     */
    public final static String executeTrans= "/kettle/executeTrans/?rep=&trans=/home/ubuntu/samples/enableWebService.ktr&";


    public final static String executeJobs= "/kettle/executeJob/?rep=PentahoRepository&job=/home/addr/ADDR_FULL_FLOW/MAIN.kjb&level=Debug&";



}
