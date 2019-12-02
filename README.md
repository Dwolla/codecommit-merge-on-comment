# CodeCommit Merge on Comment

Listens to CodeCommit PR Comment feeds, and merge pull requests when someone comments approvingly

```shell
serverless deploy \
  --region eu-east-1 \
  --account $(aws sts get-caller-identity | jq -r .Account) \
  --bucket my-amazing-s3-bucket \
  --stage sandbox
```

## Workflow

An IAM user needs to create both the original pull request and the approving comment. (Users who have assumed roles are blocked because it is difficult to determine the actual underlying user.)  A user other than the one who created the original pull request can post a comment containing `lgtm` (“looks good to me!”) or `approved`. These strings need to be the entire content of the comment, but the casing doesn't matter (i.e. `LGTM` or `LgTm` are fine too). 