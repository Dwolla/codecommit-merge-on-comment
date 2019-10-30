# CodeCommit Merge on Comment

Listens to CodeCommit PR Comment feeds, and merge pull requests when someone comments approvingly

```shell
serverless deploy \
  --region eu-east-1 \
  --account $(aws sts get-caller-identity | jq -r .Account) \
  --bucket my-amazing-s3-bucket \
  --stage sandbox
```
