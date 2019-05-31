

// TODO: Is all this even necessary? Should Calliope be using LDAP itself instead of having a helper class for it?

/*
Authentication for Calliope done by using LDAP to interface with Open Distro on the Cyverse servers.

Will replace old "CalliopeAuth" system.
 */

public class ldapAuth
{
    // TODO: Copied from CalliopeAuth.java, is this needed?

    // The port we get data from
    private static final Integer INPUT_PORT = 5235;
    // The string containing the host address that we connect to
    private static final String CYVERSE_HOST = "data.cyverse.org";
    // The string containing the host address of CyVerse
    private static final Integer CYVERSE_PORT = 1247;
    // The directory that each user has as their home directory
    private static final String CYVERSE_HOME_DIRECTORY = "/iplant/home/";
    // Each user is part of the iPlant zone
    private static final String CYVERSE_ZONE = "iplant";
}