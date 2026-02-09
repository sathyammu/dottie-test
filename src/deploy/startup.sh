#!/bin/sh
set -xe
cp /extras/docflow-startup-bundle/sshd_config /etc/ssh/
service ssh start

ls -al /pw-browsers
ls -al /extras
exec java -DPLAYWRIGHT_BROWSERS_PATH=/pw-browsers  -jar /extras/docflow-startup-bundle/docflow-rule-engine-#{Octopus.Action.Package[vallia-doc-flow-rule-engine].PackageVersion}.jar