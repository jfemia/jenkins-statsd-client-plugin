package io.jenkins.plugins.statsdclient;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class StatsDBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String metric = "Test";
    final String prefix = "Prefix";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new StatsDBuilder(metric));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new StatsDBuilder(metric), project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        StatsDBuilder builder = new StatsDBuilder(metric);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Incrementing " + metric, build);
    }
    
    @Test
    public void testBuildPrefix() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        StatsDBuilder builder = new StatsDBuilder(metric);
        builder.setPrefix(prefix);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Incrementing " + prefix + "." + metric, build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  statsd '" + metric + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "Incrementing " + metric;
        jenkins.assertLogContains(expectedString, completedBuild);
    }
    
    @Test
    public void testScriptedPipelinePrefix() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  statsd metric: '" + metric + "', prefix: '" + prefix + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "Incrementing " + prefix + "." + metric;
        jenkins.assertLogContains(expectedString, completedBuild);
    }

}