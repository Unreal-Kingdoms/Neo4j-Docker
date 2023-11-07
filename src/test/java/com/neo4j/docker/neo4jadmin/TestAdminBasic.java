package com.neo4j.docker.neo4jadmin;

import com.neo4j.docker.utils.StartupDetector;
import com.neo4j.docker.utils.TestSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class TestAdminBasic
{
    private static final Logger log = LoggerFactory.getLogger( TestAdminBasic.class );

    @Test
    void testCannotRunNeo4j()
    {
        GenericContainer admin = new GenericContainer( TestSettings.ADMIN_IMAGE_ID );
        admin.withEnv( "NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes" )
             .withExposedPorts( 7474, 7687 )
             .withLogConsumer( new Slf4jLogConsumer( log ) )
             .withCommand( "neo4j", "console" );
        StartupDetector.makeContainerWaitUntilFinished( admin, Duration.ofSeconds(30) );

        Assertions.assertThrows( ContainerLaunchException.class, admin::start );
        admin.stop();
    }
    @Test
    void testLicenseAcceptanceRequired_Neo4jAdmin()
    {
        Assumptions.assumeTrue( TestSettings.EDITION == TestSettings.Edition.ENTERPRISE,
                                "No license checks for community edition");

        String logsOut;
        try(GenericContainer container = new GenericContainer( TestSettings.ADMIN_IMAGE_ID )
                .withLogConsumer( new Slf4jLogConsumer( log ) ) )
        {
            StartupDetector.makeContainerWaitUntilFinished( container, Duration.ofSeconds(30) );
            // container start should fail due to licensing.
            Assertions.assertThrows( ContainerLaunchException.class, container::start,
                                     "Neo4j did not notify about accepting the license agreement" );
            logsOut = container.getLogs();
        }
        // double check the container didn't warn and start neo4j anyway
        Assertions.assertTrue( logsOut.contains( "must accept the license" ),
                               "Neo4j did not notify about accepting the license agreement" );
        Assertions.assertFalse( logsOut.contains( "Remote interface available" ),
                                "Neo4j was started even though the license was not accepted" );
    }
}
