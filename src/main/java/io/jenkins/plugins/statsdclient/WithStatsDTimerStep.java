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

import com.google.common.collect.ImmutableSet;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import hudson.Extension;
import hudson.model.TaskListener;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Set;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class WithStatsDTimerStep extends Step implements Serializable {
    private final @Nonnull String prefix;
    private Calendar startTime = null;
    
    @DataBoundConstructor public WithStatsDTimerStep(@Nonnull String prefix) {
        this.prefix = prefix;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public Calendar getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }
    
    public static class Execution extends StepExecution {
        private static final long serialVersionUID = 1;
        private WithStatsDTimerStep step;
        
        public Execution(WithStatsDTimerStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override public boolean start() throws Exception {
            getContext().get(TaskListener.class).getLogger().println("Starting StatsD Timing (prefix: " + step.getPrefix() + ")");
            step.setStartTime(Calendar.getInstance());
            getContext().newBodyInvoker().withCallback(new Callback(step)).start();
            return false;
        }
        
        @Override public void stop(@Nonnull Throwable cause) throws Exception {
            getContext().get(TaskListener.class).getLogger().println("Stopping StatsD Timing (prefix: " + step.getPrefix() + ")");
        }
    }
    
    private static class Callback extends BodyExecutionCallback.TailCall {
        private static final long serialVersionUID = 1;
        private final WithStatsDTimerStep step;
        
        Callback(WithStatsDTimerStep step) {
            this.step = step;
        }
        
        @Override protected void finished(StepContext context) throws Exception {
            PrintStream logger = context.get(TaskListener.class).getLogger();
            logger.println("Finished StatsD Timing (prefix: " + step.getPrefix() + ")");
            Calendar endTime = Calendar.getInstance();
            long ms = endTime.getTimeInMillis() - step.getStartTime().getTimeInMillis();
            long seconds = ms / 1000;
            logger.println("Steps completed in " + seconds + " seconds");
            
            try {
                StatsDClient statsd = StatsDClientFactory.newInstance(step.getPrefix());
                statsd.recordExecutionTime("execution", ms);
            } catch(StatsDClientException ex) {
                logger.println("Could not broadcast to StatsD: " + ex.getMessage());
            }
        }
    }
    
    @Extension public static class DescriptorImpl extends StepDescriptor {
        @Override public String getFunctionName() {
            return "withStatsDTimer";
        }
        
        @Override public String getDisplayName() {
            return "Run build steps and report their elapsed time to StatsD";
        }
        
        @Override public boolean takesImplicitBlockArgument() {
            return true;
        }
        
        @Override public boolean isAdvanced() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class);
        }
    }
}
