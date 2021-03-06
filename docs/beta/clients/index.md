# Client Configuration

The plugin allows you to configure the Secrets Manager client(s) that it uses to access secrets.

<table>
    <thead>
        <tr>
            <th>Client Configuration</th>
            <th>Same-account access</th>
            <th>Cross-account access</th>
            <th>Multiple clients</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Default</td>
            <td>Yes</td>
            <td>No</td>
            <td>No</td>
        </tr>
        <tr>
            <td>Custom</td>
            <td>Yes</td>
            <td>Yes</td>
            <td>Yes</td>
        </tr>
    </tbody>
</table>

## Default

The default client performs *same-account* secret access, using the default `AWSCredentialsProvider` chain to authenticate and authorize with Secrets Manager.

No extra Jenkins configuration is necessary to use the default client configuration. This is the simplest option and should serve most use cases.

## Custom

The plugin can access more (or different) secrets when you enable the custom client configuration.

The most common use case is to access secrets in other accounts using [IAM cross-account roles](https://docs.aws.amazon.com/IAM/latest/UserGuide/tutorial_cross-account-with-roles.html). In this setup, Jenkins performs *same-account* secrets access with its IAM principal's *implicit* role, and performs *cross-account* secrets access with *explicit* roles.

With custom clients, you can decide:
 
- Whether *same-account* secrets access should be enabled.
- Which *cross-account* calls you want to make.

The plugin de-duplicates client configurations (where two clients have the same credentials provider, endpoint configuration, and region) when you save the Jenkins configuration. This helps you to avoid redundant clients.

To set this up, for each client:

1. Create the associated IAM roles and policies in AWS.
2. Ensure that Jenkins can assume the role and retrieve secrets.
3. Add the client to the `clients` list in the plugin configuration.

**Example:** Jenkins is installed in an AWS account. You want Jenkins to access secrets in the same account, and also secrets in account ID 111111111111:

```yaml
unclassified:
  awsCredentialsProvider:
    beta:
      clients:
        - {}  # Client with default settings
        - credentialsProvider:
            assumeRole:
              roleArn: "arn:aws:iam::111111111111:role/foo"
              roleSessionName: "jenkins"
```

### Options

#### Credentials Providers

The plugin supports the following `AWSCredentialsProvider` implementations to authenticate and authorize with Secrets Manager.

Note: This is not the same thing as a Jenkins `CredentialsProvider`.

##### Default

This uses the standard AWS credentials lookup chain. You will typically use this provider for *same-account* secrets access.

##### Profile

This allows you to use named AWS profiles (which then reference IAM roles) from the standard AWS user configuration file. You will typically use this provider for *cross-account* secrets access, when you can specify the roles in `~/.aws/config`.

```yaml
unclassified:
  awsCredentialsProvider:
    beta:
      clients:
        - credentialsProvider:
            profile:
              profileName: "staging"
        - credentialsProvider:
            profile:
              profileName: "production"
```

##### STS AssumeRole

This allows you to specify IAM roles inline within Jenkins. You will typically use this provider for *cross-account* secrets access, when you cannot specify the roles in `~/.aws/config`.

```yaml
unclassified:
  awsCredentialsProvider:
    beta:
      clients:
        - credentialsProvider:
            assumeRole:
              roleArn: "arn:aws:iam::111111111111:role/foo"
              roleSessionName: "jenkins"
        - credentialsProvider:
            assumeRole:
              roleArn: "arn:aws:iam::222222222222:role/bar"
              roleSessionName: "jenkins"
```

#### Endpoint Configuration

You can set the AWS endpoint configuration for each client.

```yaml
unclassified:
  awsCredentialsProvider:
    beta:
      clients:
        - endpointConfiguration:
            serviceEndpoint: "http://localhost:4584"
            signingRegion: "us-east-1"
```

#### Region

You can set the AWS region for each client. You will typically do this when the target account is in a different region to Jenkins.

```yaml
unclassified:
  awsCredentialsProvider:
    beta:
      clients:
        - region: "us-east-1"
```

### Considerations

**Do not add more clients than necessary.** Each additional client creates another set of HTTP requests to retrieve secrets. This increases the time to populate the credential list. It also increases the risk of service degradation, as any of those requests could fail.

### Secret Namespacing

Secrets in different accounts may have the same name. To allow them to co-exist within Jenkins, credentials from different client types use different secret attributes for their IDs:

<table>
    <thead>
        <tr>
            <th>Client</th>
            <th>Credential ID</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Default</td>
            <td>Secret Name</td>
        </tr>
        <tr>
            <td>Custom</td>
            <td>Secret ARN</td>
        </tr>
    </tbody>
</table>
