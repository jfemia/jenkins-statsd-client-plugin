/*
 * The MIT License
 *
 * Copyright 2018 J. Femia
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.statsdclient;

import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class StatsDBuilder extends Builder implements SimpleBuildStep {
    private final String metric;
    private String prefix;
    
    @DataBoundConstructor
    public StatsDBuilder(String metric) {
        this.prefix = "";
        this.metric = metric;
    }

    public String getPrefix() {
        return prefix;
    }
    
    @DataBoundSetter
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMetric() {
        return metric;
    }
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String p = prefix;
        if(!p.isEmpty()) {
            p += ".";
        }
        
        listener.getLogger().println("Incrementing " + p + metric);
        try {
            StatsDClient statsd = StatsDClientFactory.newInstance(prefix);
            statsd.increment(metric);
        } catch(StatsDClientException ex) {
            listener.getLogger().println("Could not broadcast to StatsD: " + ex.getMessage());
        }   
    }

    @Symbol("statsd")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.StatsDBuilder_DescriptorImpl_DisplayName();
        }
    }
}
