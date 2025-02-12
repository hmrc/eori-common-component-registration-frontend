#!/bin/bash

sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dconfig.resource=local.conf