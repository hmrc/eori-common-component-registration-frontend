#!/bin/bash

sbt clean coverage it:test coverageReport
