package com.level11data.databricks.job;

import com.level11data.databricks.client.HttpException;
import com.level11data.databricks.client.JobsClient;
import com.level11data.databricks.cluster.InteractiveCluster;
import com.level11data.databricks.client.entities.jobs.*;
import com.level11data.databricks.job.run.InteractiveNotebookJobRun;
import com.level11data.databricks.job.run.JobRunException;
import com.level11data.databricks.library.Library;
import com.level11data.databricks.workspace.Notebook;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractiveNotebookJob extends AbstractInteractiveJob implements NotebookJob {

    private JobsClient _client;
    public final Notebook Notebook;
    public final Map<String,String> BaseParameters;

    public InteractiveNotebookJob(JobsClient client,
                                  InteractiveCluster cluster,
                                  JobSettingsDTO jobSettingsDTO,
                                  Notebook notebook) throws JobConfigException {
        this(client, cluster, jobSettingsDTO, notebook, null);
    }

    public InteractiveNotebookJob(JobsClient client,
                    InteractiveCluster cluster,
                    JobSettingsDTO jobSettingsDTO,
                    Notebook notebook,
                    List <Library> libraries) throws JobConfigException {
        super(client, cluster, null, jobSettingsDTO, libraries);

        _client = client;

        //Validate DTO for this Job Type
        JobValidation.validateInteractiveNotebookJob(jobSettingsDTO);

        Notebook = notebook;
        if(jobSettingsDTO.NotebookTask.BaseParameters == null) {
            BaseParameters = Collections.unmodifiableMap(new HashMap<>());
        } else {
            BaseParameters = Collections.unmodifiableMap(jobSettingsDTO.NotebookTask.BaseParameters);
        }
    }

    public InteractiveNotebookJob(JobsClient client,
                                  InteractiveCluster cluster,
                                  JobDTO jobDTO,
                                  Notebook notebook) throws JobConfigException {
        this(client, cluster, jobDTO, notebook, null);
    }

    public InteractiveNotebookJob(JobsClient client,
                    InteractiveCluster cluster,
                    JobDTO jobDTO,
                    Notebook notebook,
                    List < Library > libraries) throws JobConfigException {
        super(client, cluster, jobDTO.JobId, jobDTO.Settings, libraries);

        _client = client;

        //Validate DTO for this Job Type
        JobValidation.validateInteractiveNotebookJob(jobDTO);

        Notebook = notebook;
        if(jobDTO.Settings.NotebookTask.BaseParameters == null) {
            BaseParameters = Collections.unmodifiableMap(new HashMap<>());
        } else {
            BaseParameters = Collections.unmodifiableMap(jobDTO.Settings.NotebookTask.BaseParameters);
        }
    }

    public InteractiveNotebookJobRun run() throws JobRunException {
        return run(null);
    }

    public InteractiveNotebookJobRun run(Map<String,String> overrideParameters) throws JobRunException {
        try {
            RunNowRequestDTO runRequestDTO = new RunNowRequestDTO();
            runRequestDTO.JobId = this.Id;

            if(overrideParameters != null) {
                runRequestDTO.NotebookParams = overrideParameters;
            }
            RunNowResponseDTO response = _client.runJobNow(runRequestDTO);
            RunDTO jobRun = _client.getRun(response.RunId);
            return new InteractiveNotebookJobRun(_client, jobRun);
        } catch(HttpException e) {
            throw new JobRunException(e);
        }
    }
}
