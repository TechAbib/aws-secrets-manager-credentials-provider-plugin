package io.jenkins.plugins.credentials.secretsmanager.config;

import com.amazonaws.services.secretsmanager.model.FilterNameStringType;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Extension
@Symbol("awsCredentialsProvider")
public class PluginConfiguration extends GlobalConfiguration {

    private static final Logger LOG = Logger.getLogger(PluginConfiguration.class.getName());

    /** The Guava cache is never truly turned off, just made very short, as it needs a non-zero cache duration. */
    private static final Duration NO_CACHE = Duration.ofNanos(1);
    private static final Duration DEFAULT_CACHE = Duration.ofSeconds(300);

    private Beta beta;

    /**
     * Whether to cache the credentials or not. By default, credentials are cached for 5 minutes. Caching can be turned off for development purposes.
     */
    private Boolean cache;

    /**
     * The AWS Secrets Manager endpoint configuration. If this is null, the default will be used. If
     * this is specified, the user's override will be used.
     */
    private EndpointConfiguration endpointConfiguration;

    @Deprecated
    private transient Filters filters;

    private ListSecrets listSecrets;

    public PluginConfiguration() {
        load();
    }

    public static PluginConfiguration getInstance() {
        return all().get(PluginConfiguration.class);
    }

    public static Duration normalize(Boolean cache) {
        if (cache == null || cache) {
            LOG.config("CredentialsProvider cache enabled");
            return DEFAULT_CACHE;
        } else {
            LOG.config("CredentialsProvider cache disabled");
            return NO_CACHE;
        }
    }

    protected Object readResolve() {
        if (filters != null && filters.getTag() != null) {
            final Tag tag = filters.getTag();
            final Filter tagKey = new Filter(FilterNameStringType.TagKey.toString(), Collections.singletonList(new Value(tag.getKey())));
            final Filter tagValue = new Filter(FilterNameStringType.TagValue.toString(), Collections.singletonList(new Value(tag.getValue())));
            final List<Filter> filters = Arrays.asList(tagKey, tagValue);
            listSecrets = new ListSecrets(filters);
        }

        return this;
    }

    public Beta getBeta() {
        return beta;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setBeta(Beta beta) {
        this.beta = beta;
        save();
    }

    public Boolean getCache() {
        return cache;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setCache(Boolean cache) {
        this.cache = cache;
        save();
    }

    public EndpointConfiguration getEndpointConfiguration() {
        return endpointConfiguration;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
        this.endpointConfiguration = endpointConfiguration;
        save();
    }

    public ListSecrets getListSecrets() {
        return listSecrets;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setListSecrets(ListSecrets listSecrets) {
        this.listSecrets = listSecrets;
        save();
    }

    @Override
    public synchronized boolean configure(StaplerRequest req, JSONObject json) {
        // This method is unnecessary, except to apply the following workaround.
        // Workaround: Set any optional struct fields to null before binding configuration.
        // https://groups.google.com/forum/#!msg/jenkinsci-dev/MuRJ-yPRRoo/AvoPZAgbAAAJ
        this.beta = null;
        this.endpointConfiguration = null;
        this.listSecrets = null;

        req.bindJSON(this, json);
        save();
        return true;
    }
}
