package com.level11data.databricks.library;

import com.level11data.databricks.client.HttpException;
import com.level11data.databricks.client.LibrariesClient;
import com.level11data.databricks.client.entities.libraries.*;
import com.level11data.databricks.cluster.ClusterLibrary;
import com.level11data.databricks.cluster.InteractiveCluster;
import com.level11data.databricks.library.util.LibraryHelper;

public class CranLibrary extends PublishedLibrary {
    private final LibrariesClient _client;

    public final String PackageName;
    public final String RepoOverride;

    public CranLibrary(LibrariesClient client, String packageName) {
        super(client);
        _client = client;
        PackageName = packageName;
        RepoOverride = null;
    }

    public CranLibrary(LibrariesClient client, String packageName, String repo) {
        super(client);
        _client = client;
        PackageName = packageName;
        RepoOverride = repo;
    }

    public LibraryStatus getClusterStatus(InteractiveCluster cluster) throws HttpException, LibraryConfigException {
        ClusterLibraryStatusesDTO libStatuses = _client.getClusterStatus(cluster.Id);

        //find library status for this library
        for (LibraryFullStatusDTO libStat : libStatuses.LibraryStatuses) {
            if(libStat.Library.Cran != null) {
                if(libStat.Library.Cran.Package.equals(this.PackageName)) {
                    return new LibraryStatus(libStat);
                }
            }
        }
        throw new LibraryConfigException("CRAN Library " + this.PackageName +
                " Not Associated With Cluster Id " + cluster.Id);
    }

    public ClusterLibrary install(InteractiveCluster cluster) throws HttpException {
        _client.installLibraries(createLibraryRequest(cluster, LibraryHelper.createLibraryDTO(this)));
        return new ClusterLibrary(cluster, this);
    }

    public void uninstall(InteractiveCluster cluster) throws HttpException {
        _client.uninstallLibraries(createLibraryRequest(cluster, LibraryHelper.createLibraryDTO(this)));
    }

}
