package com.level11data.databricks.client;

import com.level11data.databricks.client.entities.dbfs.*;
import com.level11data.databricks.client.entities.workspace.*;
import com.level11data.databricks.cluster.*;
import com.level11data.databricks.cluster.builder.*;
import com.level11data.databricks.config.DatabricksClientConfiguration;
import com.level11data.databricks.client.entities.jobs.*;
import com.level11data.databricks.client.entities.clusters.*;
import com.level11data.databricks.dbfs.*;
import com.level11data.databricks.job.*;
import com.level11data.databricks.job.builder.*;
import com.level11data.databricks.job.run.*;
import com.level11data.databricks.library.*;
import com.level11data.databricks.util.ResourceConfigException;
import com.level11data.databricks.workspace.*;
import com.level11data.databricks.workspace.builder.ScalaNotebookBuilder;
import com.level11data.databricks.workspace.util.WorkspaceHelper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class DatabricksSession {
    protected final URI Url;

    private final DatabricksClientConfiguration _databricksClientConfig;

    private final ClientConfig _clientConfig = new ClientConfig().register(new JacksonFeature());
    private final Client _httpClient = ClientBuilder.newClient(_clientConfig);
    private HttpAuthenticationFeature _userPassAuth;

    private ClustersClient _clustersClient;
    private JobsClient _jobsClient;
    private LibrariesClient _librariesClient;
    private DbfsClient _dbfsClient;
    private WorkspaceClient _workspaceClient;

    private SparkVersionsDTO _sparkVersionsDTO;
    private NodeTypesDTO _nodeTypesDTO;
    private List<SparkVersion> _sparkVersions;
    private List<NodeType> _nodeTypes;
    private WorkspaceHelper _workspaceHelper;

    public DatabricksSession(DatabricksClientConfiguration databricksConfig) throws DatabricksConfigException {
        //validate expectations of config; throw exception if not met
        validateConfig(databricksConfig);

        _databricksClientConfig = databricksConfig;
        Url = databricksConfig.getClientUrl();
    }

    public Builder getRequestBuilder(String path) {
        return getRequestBuilder(path, null);
    }

    public Builder getRequestBuilder(String path, String queryParamKey, Object queryParamValue) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(queryParamKey, queryParamValue);
        return getRequestBuilder(path, queryMap);
    }

    public Builder getRequestBuilder(String path, Map<String,Object> queryParams) {
        //TODO add DEBUG System.out.println(_httpClient.target(this.Url).path(path).getUri().toString());
//        if(queryParams != null) {
//            System.out.println("Query Params:");
//            queryParams.forEach((k,v) -> System.out.println("(" + k + "," + v + ")"));
//        }

        if(_databricksClientConfig.hasClientToken()) {
            //authenticate with token as first priority
            //System.out.println("Authenticating with TOKEN");
            if (queryParams != null) {
                WebTarget target = _httpClient.target(this.Url).path(path);

                target = applyQueryParameters(target, queryParams);

                return target
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + _databricksClientConfig.getClientToken())
                        .accept(MediaType.APPLICATION_JSON);
            } else {
                return _httpClient
                        .target(this.Url)
                        .path(path)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + _databricksClientConfig.getClientToken())
                        .accept(MediaType.APPLICATION_JSON);
            }
        } else {
            //otherwise, authenticate with username and password
            //TODO add DEBUG System.out.println("Authenticating with USERNAME and PASSWORD");
            if(queryParams != null) {
                WebTarget target = _httpClient.target(this.Url).path(path);

                target = applyQueryParameters(target, queryParams);

                return target
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON);
            } else {
                return _httpClient
                        .target(this.Url)
                        .path(path)
                        .register(getUserPassAuth())
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON);
            }
        }
    }

    private WebTarget applyQueryParameters(WebTarget target, Map<String,Object> queryParams) {
        for(Map.Entry<String,Object> queryParam : queryParams.entrySet()) {
            //System.out.println("Applying Query Praram to HTTP Request: "+queryParam.getKey() + "," + queryParam.getValue());
            target = target.queryParam(queryParam.getKey(), queryParam.getValue());
        }
        return target;
    }

    private void validateConfig(DatabricksClientConfiguration databricksConfig) throws DatabricksConfigException {
        if(!databricksConfig.hasClientToken() && !databricksConfig.hasClientUsername()) {
            throw new DatabricksConfigException("Neither token nor username in DatabricksConfig");
        } else if(databricksConfig.hasClientUsername() && !databricksConfig.hasClientPassword()) {
            throw new DatabricksConfigException("Password not in DatabricksConfig");
        }
    }

    private HttpAuthenticationFeature getUserPassAuth() {
        if(_userPassAuth == null) {
            _userPassAuth = HttpAuthenticationFeature.basicBuilder()
                    .credentials(_databricksClientConfig.getClientUsername()
                            , _databricksClientConfig.getClientPassword())
                    .build();
        }
        return _userPassAuth;
    }

    public ClustersClient getClustersClient() {
        if(_clustersClient == null) {
            _clustersClient =  new ClustersClient(this);
        }
        return _clustersClient;
    }

    public LibrariesClient getLibrariesClient() {
        if(_librariesClient == null) {
            _librariesClient =  new LibrariesClient(this);
        }
        return _librariesClient;
    }

    public JobsClient getJobsClient() {
        if(_jobsClient == null) {
            _jobsClient = new JobsClient(this);
        }
        return _jobsClient;
    }

    public DbfsClient getDbfsClient() {
        if(_dbfsClient == null) {
            _dbfsClient = new DbfsClient(this);
        }
        return _dbfsClient;
    }

    public WorkspaceClient getWorkspaceClient() {
        if(_workspaceClient == null) {
            _workspaceClient = new WorkspaceClient(this);
        }
        return _workspaceClient;
    }

    public InteractiveClusterBuilder createInteractiveCluster(String name, Integer numWorkers)  {
        return new InteractiveClusterBuilder(getClustersClient(), name, numWorkers);
    }

    public InteractiveClusterBuilder createInteractiveCluster(String name, Integer minWorkers, Integer maxWorkers) {
        return new InteractiveClusterBuilder(getClustersClient(), name, minWorkers, maxWorkers);
    }

    public AutomatedClusterBuilder createClusterSpec(Integer numWorkers) {
        return new AutomatedClusterBuilder(getClustersClient(), numWorkers);
    }

    public AutomatedClusterBuilder createClusterSpec( Integer minWorkers, Integer maxWorkers) {
        return new AutomatedClusterBuilder(getClustersClient(), minWorkers, maxWorkers);
    }


    private void refreshSparkVersionsDTO() throws HttpException {
        _sparkVersionsDTO = getClustersClient().getSparkVersions();
    }

    private SparkVersionsDTO getOrRequestSparkVersionsDTO() throws HttpException {
        if(_sparkVersionsDTO == null) {
            refreshSparkVersionsDTO();
        }
        return _sparkVersionsDTO;
    }

    public SparkVersion getDefaultSparkVersion() throws HttpException, ClusterConfigException  {
        return getSparkVersionByKey(getOrRequestSparkVersionsDTO().DefaultVersionKey);
    }

    private void initSparkVersions() throws HttpException {
        String defaultSparkVersionKey = getOrRequestSparkVersionsDTO().DefaultVersionKey;
        boolean isDefaultKeyInList = false;

        List<SparkVersionDTO> sparkVersionsDTO = getOrRequestSparkVersionsDTO().Versions;
        ArrayList<SparkVersion> sparkVersions = new ArrayList<>();

        for(SparkVersionDTO svDTO : sparkVersionsDTO) {
            sparkVersions.add(new SparkVersion(svDTO.Key, svDTO.Name));
            if(svDTO.Key.equals(defaultSparkVersionKey)) {
                isDefaultKeyInList = true;
            }
        }
        //It's possible that the Default Spark Version is not included in the list
        // possibly because it is deprecated.  If so, add it to the list with the key
        // as both the key and the value (since the value cannot be derived)
        if(!isDefaultKeyInList) {
            sparkVersions.add(new SparkVersion(defaultSparkVersionKey, defaultSparkVersionKey));
        }
        _sparkVersions = sparkVersions;
    }

    public List<SparkVersion> getSparkVersions() throws HttpException  {
        if(_sparkVersions == null) {
            initSparkVersions();
        }
        return _sparkVersions;
    }

    public SparkVersion getSparkVersionByKey(String key) throws ClusterConfigException {
        try {
            List<SparkVersion> sparkVersions = getSparkVersions();
            for (SparkVersion sv : sparkVersions) {
                if(sv.Key.equals(key)) {
                    return sv;
                }
            }
        } catch(HttpException e) {
            throw new ClusterConfigException(e);
        }
        throw new ClusterConfigException("No SparkVersion Found For Key "+key);
    }

    private void refreshNodeTypesDTO() throws HttpException {
        _nodeTypesDTO = getClustersClient().getNodeTypes();
    }

    private NodeTypesDTO getOrRequestNodeTypesDTO() throws HttpException {
        if(_nodeTypesDTO == null) {
            refreshNodeTypesDTO();
        }
        return _nodeTypesDTO;
    }

    public NodeType getDefaultNodeType() throws HttpException, ClusterConfigException {
        return getNodeTypeById(getOrRequestNodeTypesDTO().DefaultNodeTypeId);
    }

    private void initNodeTypes() throws ClusterConfigException {
        try {
            List<NodeTypeDTO> nodeTypesInfoListDTO
                    = getClustersClient().getNodeTypes().NodeTypes;

            ArrayList<NodeType> nodeTypesList = new ArrayList<>();

            for(NodeTypeDTO nt : nodeTypesInfoListDTO) {
                nodeTypesList.add(new NodeType(nt));
            }
            _nodeTypes = nodeTypesList;
        } catch(HttpException e) {
            throw new ClusterConfigException(e);
        }
    }

    public List<NodeType> getNodeTypes() throws ClusterConfigException  {
        if(_nodeTypes == null) {
            initNodeTypes();
        }
        return _nodeTypes;
    }

    private WorkspaceHelper getWorkspaceHelper() {
        if(_workspaceHelper == null) {
            _workspaceHelper = new WorkspaceHelper(getWorkspaceClient());
        }
        return _workspaceHelper;
    }

    public NodeType getNodeTypeById(String id) throws ClusterConfigException {
        List<NodeType> nodeTypes = getNodeTypes();

        for (NodeType nt : nodeTypes) {
            if(nt.Id.equals(id)) {
                return nt;
            }
        }

        //No NodeTypeDTO found
        throw new ClusterConfigException("No NodeType Found For Id "+id);
    }

    public String getDefaultZone()  throws HttpException {
        return getClustersClient().getZones().DefaultZone;
    }

    public String[] getZones() throws HttpException  {
        return getClustersClient().getZones().Zones;
    }

    public Iterator<InteractiveCluster> listClusters() throws HttpException {
        ClustersClient client = getClustersClient();
        ClusterInfoDTO[] clusterInfoDTOs = client.listClusters().Clusters;
        return new ClusterIter(client, clusterInfoDTOs);
    }

    public InteractiveCluster getCluster(String id) throws ClusterConfigException {
        try {
            ClustersClient client = getClustersClient();
            ClusterInfoDTO clusterInfoDTO = client.getCluster(id);
            return new InteractiveCluster(client, clusterInfoDTO);
        } catch(HttpException e) {
            throw new ClusterConfigException(e);
        }
    }

    public Job getJob(long jobId) throws JobConfigException {
        try {
            JobsClient client = getJobsClient();
            JobDTO jobDTO = client.getJob(jobId);

            if(jobDTO.isInteractive() && jobDTO.isNotebookJob()) {
                InteractiveCluster cluster = getCluster(jobDTO.Settings.ExistingClusterId);
                Notebook notebook = getNotebook(jobDTO.Settings.NotebookTask.NotebookPath);
                return new InteractiveNotebookJob(client, cluster, jobDTO, notebook);
            } else if(jobDTO.isAutomated() && jobDTO.isNotebookJob()) {
                return new AutomatedNotebookJob(client, jobDTO);
            } else if(jobDTO.isInteractive() && jobDTO.isJarJob()) {
                InteractiveCluster cluster = getCluster(jobDTO.Settings.ExistingClusterId);
                return new InteractiveJarJob(client,cluster, jobDTO);
            } else if(jobDTO.isAutomated() && jobDTO.isJarJob()) {
                return new AutomatedJarJob(client, jobDTO);
            } else if(jobDTO.isInteractive() && jobDTO.isPythonJob()) {
                InteractiveCluster cluster = getCluster(jobDTO.Settings.ExistingClusterId);
                PythonScript pythonScript = getPythonScript(new URI(jobDTO.Settings.SparkPythonTask.PythonFile));
                return new InteractivePythonJob(client, cluster, pythonScript, jobDTO);
            } else if(jobDTO.isAutomated() && jobDTO.isPythonJob()) {
                PythonScript pythonScript = getPythonScript(new URI(jobDTO.Settings.SparkPythonTask.PythonFile));
                return new AutomatedPythonJob(client, pythonScript, jobDTO);
            } else if(jobDTO.isAutomated() && jobDTO.isSparkSubmitJob()) {
                return new AutomatedSparkSubmitJob(client, jobDTO);
            }
        } catch(HttpException e) {
            throw new JobConfigException(e);
        } catch(ClusterConfigException e) {
            throw new JobConfigException(e);
        } catch(URISyntaxException e) {
            throw new JobConfigException(e);
        } catch(ResourceConfigException e) {
            throw new JobConfigException(e);
        } catch(WorkspaceConfigException e) {
            throw new JobConfigException(e);
        }
        //No valid Job Type was found
        throw new JobConfigException("Unsupported Job Type");
    }

    public JobRun getRun(long runId) throws HttpException, JobRunException {
        JobsClient client = getJobsClient();
        RunDTO runDTO = client.getRun(runId);

        if(runDTO.isInteractive() && runDTO.isNotebookJob()) {
            return new InteractiveNotebookJobRun(client, runDTO);
        } else {
            throw new JobRunException("Unsupported Job Type");  //TODO add better exception handling
        }
    }

    public AutomatedNotebookJobBuilder createJob(Notebook notebook) {
        return new AutomatedNotebookJobBuilder(getJobsClient(), notebook);
    }

    public AutomatedNotebookJobBuilder createJob(Notebook notebook, Map<String,String> parameters) {
        return new AutomatedNotebookJobBuilder(getJobsClient(), notebook, parameters);
    }

    public AutomatedJarJobBuilder createJob(JarLibrary jarLibrary, String mainClassName, List<String> parameters) {
        return new AutomatedJarJobBuilder(getJobsClient(), mainClassName, jarLibrary, parameters);
    }

    public AutomatedJarJobBuilder createJob(JarLibrary jarLibrary, String mainClassName) {
        return new AutomatedJarJobBuilder(getJobsClient(), mainClassName, jarLibrary);
    }

    public AutomatedJarJobBuilder createJob(JarLibrary jarLibrary, String mainClassName, File jarFile) {
        return new AutomatedJarJobBuilder(getJobsClient(), mainClassName, jarLibrary, jarFile);
    }

    public AutomatedJarJobBuilder createJob(JarLibrary jarLibrary, String mainClassName, File jarFile, List<String> parameters) {
        return new AutomatedJarJobBuilder(getJobsClient(), mainClassName, jarLibrary, jarFile, parameters);
    }

    public AutomatedPythonJobBuilder createJob(PythonScript pythonScript, File pythonFile, List<String> parameters) {
        return new AutomatedPythonJobBuilder(getJobsClient(), pythonScript, pythonFile, parameters);
    }

    public AutomatedPythonJobBuilder createJob(PythonScript pythonScript, File pythonFile) {
        return new AutomatedPythonJobBuilder(getJobsClient(), pythonScript, pythonFile);
    }

    public AutomatedPythonJobBuilder createJob(PythonScript pythonScript) {
        return new AutomatedPythonJobBuilder(getJobsClient(), pythonScript);
    }

    public AutomatedPythonJobBuilder createJob(PythonScript pythonScript, List<String> parameters) {
        return new AutomatedPythonJobBuilder(getJobsClient(), pythonScript, parameters);
    }

    public AutomatedSparkSubmitJobBuilder createJob(List<String> parameters) {
        return new AutomatedSparkSubmitJobBuilder(getJobsClient(), parameters);
    }

    public void putDbfsFile(File file, String dbfsPath,boolean overwrite) throws IOException, HttpException {
        DbfsHelper.putFile(getDbfsClient(), file, dbfsPath, overwrite);
    }

    public void putDbfsFile(File file, String dbfsPath) throws IOException, HttpException {
        DbfsHelper.putFile(getDbfsClient(), file, dbfsPath);
    }

    public byte[] getDbfsObject(String dbfsPath) throws IOException, HttpException {
        return DbfsHelper.getObject(getDbfsClient(), dbfsPath);
    }

    public DbfsFileInfo getDbfsObjectStatus(String dbfsPath) throws HttpException {
        return new DbfsFileInfo(getDbfsClient().getStatus(dbfsPath));
    }

    public void deleteDbfsObject(String dbfsPath, boolean recursive) throws HttpException {
        DbfsDeleteRequestDTO dbfsDeleteRequestDTO = new DbfsDeleteRequestDTO();
        dbfsDeleteRequestDTO.Path = dbfsPath;
        dbfsDeleteRequestDTO.Recursive = recursive;
        getDbfsClient().delete(dbfsDeleteRequestDTO);
    }

    public void moveDbfsObject(String fromPath, String toPath) throws HttpException {
        getDbfsClient().move(fromPath, toPath);
    }

    public void mkdirsDbfs(String path) throws HttpException {
        getDbfsClient().mkdirs(path);
    }

    public ArrayList<DbfsFileInfo> listDbfs(String path) throws HttpException {
        DbfsListResponseDTO dbfsListResponseDTO = getDbfsClient().list(path);
        ArrayList<DbfsFileInfo> fileList = new ArrayList<>();

        for (FileInfoDTO fileInfo : dbfsListResponseDTO.Files) {
            fileList.add(new DbfsFileInfo(fileInfo));
        }

        return fileList;
    }

    public JarLibrary getJarLibrary(URI uri) throws LibraryConfigException {
        return new JarLibrary(getLibrariesClient(), uri);
    }

    public EggLibrary getEggLibrary(URI uri) throws LibraryConfigException {
        return new EggLibrary(getLibrariesClient(), uri);
    }

    public MavenLibrary getMavenLibrary(String coordinates) throws LibraryConfigException {
        return new MavenLibrary(getLibrariesClient(), coordinates);
    }

    public MavenLibrary getMavenLibrary(String coordinates,
                                        String repo) throws LibraryConfigException {
        return new MavenLibrary(getLibrariesClient(), coordinates, repo);
    }

    public MavenLibrary getMavenLibrary(String coordinates,
                                        String repo,
                                        String[] exclusions) throws LibraryConfigException {
        return new MavenLibrary(getLibrariesClient(), coordinates, repo, exclusions);
    }

    public MavenLibrary getMavenLibrary(String coordinates,
                                        String[] exclusions) throws LibraryConfigException {
        return new MavenLibrary(getLibrariesClient(), coordinates, exclusions);
    }

    public PyPiLibrary getPyPiLibrary(String packageName) throws LibraryConfigException {
        return new PyPiLibrary(getLibrariesClient(), packageName);
    }

    public PyPiLibrary getPyPiLibrary(String packageName, String repo) throws LibraryConfigException {
        return new PyPiLibrary(getLibrariesClient(), packageName, repo);
    }

    public CranLibrary getCranLibrary(String packageName) throws LibraryConfigException {
        return new CranLibrary(getLibrariesClient(), packageName);
    }

    public CranLibrary getCranLibrary(String packageName, String repo) throws LibraryConfigException {
        return new CranLibrary(getLibrariesClient(), packageName, repo);
    }

    public PythonScript getPythonScript(URI uri) throws ResourceConfigException {
        return new PythonScript(this, uri);
    }

    public void mkdirsWorkspace(String workspacePath) throws WorkspaceConfigException {
        try{
            WorkspaceMkdirsRequestDTO workspaceMkdirsRequestDTO = new WorkspaceMkdirsRequestDTO();
            workspaceMkdirsRequestDTO.Path = workspacePath;

            getWorkspaceClient().mkdirs(workspaceMkdirsRequestDTO);
        } catch(HttpException e) {
            throw new WorkspaceConfigException(e);
        }
    }

    public void deleteWorkspaceObject(String workspacePath, boolean recursive) throws WorkspaceConfigException {
        try{
            WorkspaceDeleteRequestDTO workspaceDeleteRequestDTO = new WorkspaceDeleteRequestDTO();
            workspaceDeleteRequestDTO.Path = workspacePath;
            workspaceDeleteRequestDTO.Recursive = recursive;

            getWorkspaceClient().delete(workspaceDeleteRequestDTO);
        } catch(HttpException e) {
            throw new WorkspaceConfigException(e);
        }
    }

    public Notebook getNotebook(String workspacePath) throws WorkspaceConfigException {
        return getWorkspaceHelper().getNotebook(workspacePath);
    }

    public ScalaNotebookBuilder createScalaNotebook() throws WorkspaceConfigException {
        return new ScalaNotebookBuilder(getWorkspaceClient());
    }

    public ScalaNotebookBuilder createScalaNotebook(File file) throws WorkspaceConfigException {
        return new ScalaNotebookBuilder(getWorkspaceClient(), file);
    }
}

