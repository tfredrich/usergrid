/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * TODO: better test, this is really just a smoke test.
 */
public class ExportAppTest {
    static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );
    
    int NUM_COLLECTIONS = 10;
    int NUM_ENTITIES = 50; 
    int NUM_CONNECTIONS = 3;

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

    @org.junit.Test
    public void testBasicOperation() throws Exception {
       
        String rand = RandomStringUtils.randomAlphanumeric( 10 );
        
        // create app with some data

        String orgName = "org_" + rand;
        String appName = "app_" + rand;
        
        ExportDataCreator creator = new ExportDataCreator();
        creator.startTool( new String[] {
                "-organization", orgName,
                "-application", appName,
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort()
        }, false);
        
        long start = System.currentTimeMillis();
        
        String directoryName = "target/export" + rand;

        ExportApp exportApp = new ExportApp();
        exportApp.startTool( new String[]{
                "-application", orgName + "/" + appName,
                "-writeThreads", "100",
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
                "-outputDir", directoryName
        }, false );

        logger.info( "100 read and 100 write threads = " + (System.currentTimeMillis() - start) / 1000 + "s" );

        // result should be between 1 and 100 files of each type
        File exportDir = new File(directoryName);
        assertTrue( getFileCount( exportDir, "entities"    ) > 0 );
        assertTrue( getFileCount( exportDir, "connections" ) > 0 );
        assertTrue( getFileCount( exportDir, "entities"    ) <= 100 );
        assertTrue( getFileCount( exportDir, "connections" ) <= 100 );

        File exportDir1 = new File(directoryName + "1");
        exportApp.startTool( new String[]{
                "-application", orgName + "/" + appName,
                "-writeThreads", "5",
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
                "-outputDir", directoryName + "1"
        }, false );

        logger.info( "5 thread time = " + (System.currentTimeMillis() - start) / 1000 + "s" );

        // result should be between 1 and 10 files of each type
        assertTrue( getFileCount( exportDir1, "entities" ) > 0 );
        assertTrue( getFileCount( exportDir1, "connections" ) > 0 );
        assertTrue( getFileCount( exportDir1, "entities" ) <= 5 );
        assertTrue( getFileCount( exportDir1, "connections" ) <= 5 );
    }

    private static int getFileCount(File exportDir, final String ext ) {
        return exportDir.listFiles( new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().endsWith("." + ext);
            }
        } ).length;
    }
}