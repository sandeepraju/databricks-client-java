package com.level11data.databricks;


import com.level11data.databricks.client.DatabricksSession;
import com.level11data.databricks.cluster.InteractiveCluster;
import com.level11data.databricks.cluster.ClusterState;
import com.level11data.databricks.cluster.SparkVersion;
import com.level11data.databricks.config.DatabricksClientConfiguration;
import org.junit.Test;
import org.junit.Assert;

import java.io.InputStream;

public class InteractiveClusterTest {
    public static final String CLIENT_CONFIG_RESOURCE_NAME = "test.properties";
    public static final String DBR_VERSION = "4.3.x-scala2.11";

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream resourceStream = loader.getResourceAsStream(CLIENT_CONFIG_RESOURCE_NAME);
    DatabricksSession _databricks;
    DatabricksClientConfiguration _databricksConfig;

    public InteractiveClusterTest() throws Exception {
        loadConfigFromResource();
    }

    private void loadConfigFromResource() throws Exception {
        if(resourceStream == null) {
            throw new IllegalArgumentException("Resource Not Found: " + CLIENT_CONFIG_RESOURCE_NAME);
        }
        _databricksConfig = new DatabricksClientConfiguration(resourceStream);

        _databricks = new DatabricksSession(_databricksConfig);
    }

    @Test
    public void testSimpleClusterFixedSize() throws Exception {
        long now = System.currentTimeMillis();


        //Set cluster name to ClassName.MethodName TIMESTAMP
        String clusterName = this.getClass().getSimpleName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName() +
                " " +now;

//        for (SparkVersion sparkVersion : _databricks.getSparkVersions()) {
//            System.out.println(sparkVersion.Key);
//        }

        //Create Interactive Cluster
        InteractiveCluster cluster = _databricks.createInteractiveCluster(clusterName, 1)
                .withAutoTerminationMinutes(20)
                .withSparkVersion(DBR_VERSION)
                .withNodeType("i3.xlarge")
                .create();

        Assert.assertEquals("Simple Fixed Size InteractiveCluster Name does NOT match expected Name",
                clusterName, cluster.Name);

        String clusterId = cluster.Id;

        Assert.assertNotNull("Simple Fixed Size InteractiveCluster Id IS NULL", clusterId);

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a PENDING state after create",
                ClusterState.PENDING, cluster.getState());

        while(cluster.getState() == ClusterState.PENDING) {
          //wait until cluster is properly started
          // should not take more than 100 seconds from a cold start
          Thread.sleep(10000); //wait 10 seconds
        }

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a RUNNING state after create",
                ClusterState.RUNNING, cluster.getState());

        Assert.assertEquals("Simple Fixed Size InteractiveCluster was NOT created with expected number of executors",
                1, cluster.getExecutors().size());

        //TODO Change the Default Spark Version from "Spark 1.6.2 (Hadoop 1)"
        //Assert.assertEquals("Simple Fixed Size InteractiveCluster Spark Version does NOT match default",
        //        _databricks.getDefaultSparkVersion(), cluster.SparkVersion);

        //TODO Change the Default Node Type from "Memory Optimized"
        //Assert.assertEquals("Simple Fixed Size InteractiveCluster Node Type does NOT match default",
        //        _databricks.getDefaultNodeType(), cluster.DefaultNodeType);

        //TODO Change the Default Node Type from "Memory Optimized"
        //Assert.assertEquals("Simple Fixed Size InteractiveCluster Driver Node Type does NOT match default",
        //        _databricks.getDefaultNodeType(), cluster.DefaultNodeType);

        cluster.restart();

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a RESTARTING state after restart",
                ClusterState.RESTARTING, cluster.getState());

        while(cluster.getState() == ClusterState.RESTARTING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a RUNNING state after restart",
                ClusterState.RUNNING, cluster.getState());

        cluster = cluster.resize(0);

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a RESIZING state after resize",
                ClusterState.RESIZING, cluster.getState());

        while(cluster.getState() == ClusterState.RESIZING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a RUNNING state after resize",
                ClusterState.RUNNING, cluster.getState());

        Assert.assertEquals("Simple Fixed Size InteractiveCluster was NOT resized with expected number of executors",
                0, cluster.getExecutors().size());

        cluster.terminate();

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a TERMINATING state after terminate",
                ClusterState.TERMINATING, cluster.getState());

        while(cluster.getState() == ClusterState.TERMINATING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Fixed Size InteractiveCluster did NOT enter a TERMINATED state after terminate",
                ClusterState.TERMINATED, cluster.getState());
    }

    @Test
    public void testSimpleClusterAutoscaling() throws Exception {
        long now = System.currentTimeMillis();


        //Set cluster name to ClassName.MethodName TIMESTAMP
        String clusterName = this.getClass().getSimpleName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName() +
                " " +now;

        //Create Interactive Cluster
        Integer minWorkers = 0;
        Integer maxWorkers = 1;

        InteractiveCluster cluster = _databricks.createInteractiveCluster(clusterName, minWorkers, maxWorkers)
                .withAutoTerminationMinutes(20)
                .withSparkVersion(DBR_VERSION)
                .withNodeType("i3.xlarge")
                .create();

        Assert.assertEquals("Simple Autoscaling InteractiveCluster Name does NOT match expected NAME",
                clusterName, cluster.Name);

        String clusterId = cluster.Id;

        Assert.assertNotNull("Simple Autoscaling InteractiveCluster Id is NULL", clusterId);

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a PENDING state after create",
                ClusterState.PENDING, cluster.getState());

        while(cluster.getState() == ClusterState.PENDING) {
            //wait until cluster is properly started
            // should not take more than 100 seconds from a cold start
            Thread.sleep(10000); //wait 10 seconds
        }

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a RUNNING state after create",
                ClusterState.RUNNING, cluster.getState());

        Assert.assertEquals("Simple Autoscaling InteractiveCluster was NOT created with expected MINIMUM number of workers",
                minWorkers, cluster.AutoScale.MinWorkers);

        Assert.assertEquals("Simple Autoscaling InteractiveCluster was NOT created with expected MAXIMUM number of workers",
                maxWorkers, cluster.AutoScale.MaxWorkers);

        //TODO allow default cluster config overrides in Databricks Config File
        cluster.restart();

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a RESTARTING state after restart",
                ClusterState.RESTARTING, cluster.getState());

        while(cluster.getState() == ClusterState.RESTARTING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a RUNNING state after restart",
                ClusterState.RUNNING, cluster.getState());

        minWorkers = 1;
        maxWorkers = 2;
        cluster = cluster.resize(minWorkers, maxWorkers);

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a RESIZING state after resize",
                ClusterState.RESIZING, cluster.getState());

        while(cluster.getState() == ClusterState.RESIZING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a RUNNING state after resize",
                ClusterState.RUNNING, cluster.getState());

        Assert.assertEquals("Simple Autoscaling InteractiveCluster was NOT resized with expected MINIMUM number of workers",
                cluster.AutoScale.MinWorkers, minWorkers);

        Assert.assertEquals("Simple Autoscaling InteractiveCluster was NOT resized with expected MAXIMUM number of workers",
                cluster.AutoScale.MaxWorkers, maxWorkers);

        cluster.terminate();

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a TERMINATING state after terminate",
                ClusterState.TERMINATING, cluster.getState());

        while(cluster.getState() == ClusterState.TERMINATING) {
            Thread.sleep(5000); //wait 5 seconds
        }

        Assert.assertEquals("Simple Autoscaling InteractiveCluster did NOT enter a TERMINATED state after terminate",
                ClusterState.TERMINATED, cluster.getState());
    }


}
