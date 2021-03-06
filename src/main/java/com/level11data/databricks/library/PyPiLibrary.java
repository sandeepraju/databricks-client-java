package com.level11data.databricks.library;

import com.level11data.databricks.client.HttpException;
import com.level11data.databricks.client.LibrariesClient;
import com.level11data.databricks.client.entities.libraries.*;
import com.level11data.databricks.cluster.ClusterLibrary;
import com.level11data.databricks.cluster.InteractiveCluster;
import com.level11data.databricks.library.util.LibraryHelper;

public class PyPiLibrary extends AbstractPublishedLibrary {
    private final LibrariesClient _client;

    public final String PackageName;
    public final String RepoOverride;

    public PyPiLibrary(LibrariesClient client, String packageName) {
        super(client);
        _client = client;
        PackageName = packageName;
        RepoOverride = null;
    }

    public PyPiLibrary(LibrariesClient client, String packageName, String repo) {
        super(client);
        _client = client;
        PackageName = packageName;
        RepoOverride = repo;
    }

    public LibraryStatus getClusterStatus(InteractiveCluster cluster) throws LibraryConfigException {
        try {
            ClusterLibraryStatusesDTO libStatuses = _client.getClusterStatus(cluster.Id);

            //find library status for this library
            for (LibraryFullStatusDTO libStat : libStatuses.LibraryStatuses) {
                if(libStat.Library.PyPi != null) {
                    if(libStat.Library.PyPi.Package.equals(this.PackageName)) {
                        return new LibraryStatus(libStat);
                    }
                }
            }
        } catch (HttpException e) {
            throw new LibraryConfigException(e);
        }
        throw new LibraryConfigException("PyPi AbstractLibrary " + this.PackageName +
                " Not Associated With AbstractCluster Id " + cluster.Id);
    }

    public ClusterLibrary install(InteractiveCluster cluster) throws LibraryConfigException {
        try{
            _client.installLibraries(createLibraryRequest(cluster, LibraryHelper.createLibraryDTO(this)));
            return new ClusterLibrary(cluster, this);
        }catch(HttpException e) {
            throw new LibraryConfigException(e);
        }
    }

    public void uninstall(InteractiveCluster cluster) throws LibraryConfigException {
        try{
            _client.uninstallLibraries(createLibraryRequest(cluster, LibraryHelper.createLibraryDTO(this)));
        }catch(HttpException e) {
            throw new LibraryConfigException(e);
        }
    }
}
