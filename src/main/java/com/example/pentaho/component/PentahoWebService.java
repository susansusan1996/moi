package com.example.pentaho.component;

import org.springframework.stereotype.Component;

public class PentahoWebService  {

    /**
     * 目前的寫法是直接呼叫PentahoServer上的檔案，非PentahoRepository
     */
    public final static String executeTrans= "/kettle/executeTrans/?rep=&trans=/home/ubuntu/samples/enableWebService.ktr&";

    public final static String executeJobs= "/kettle/executeJobs/?rep=PentahoRepository&jobs=/home/addr/dev/ADDR_FULL_FLOW/1_PRE_PROCESS/test_20240216.ktr&";



}
